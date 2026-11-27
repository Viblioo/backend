package com.revibe.backend.infrastructure.adapters.output.persistence.neo4j;

import com.revibe.backend.application.ports.output.outbox.GraphSyncPort;
import com.revibe.backend.application.ports.output.outbox.GraphUnavailableException;
import com.revibe.backend.domain.model.OutboxEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.exceptions.ServiceUnavailableException;
import org.neo4j.driver.exceptions.SessionExpiredException;
import org.neo4j.driver.exceptions.TransientException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Escribe en Neo4j: aplica los eventos de graph_outbox y la reconciliación de usuarios.
 * <p>
 * Todo tiene que ser idempotente (MERGE, no CREATE): si se aplica en Neo4j pero falla al marcarlo como procesado en
 * PostgreSQL, el evento se volverá a aplicar.
 * <p>
 * Los fallos de conexión/transitorios de Neo4j se traducen a {@link GraphUnavailableException} para que el outbox no
 * gaste intentos mientras Neo4j está caído.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GraphSyncAdapter implements GraphSyncPort {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    /**
     * Crea/actualiza el nodo del evento, lo une a su creador y a su categoría. Todo con MERGE/SET: aplicarlo dos veces
     * deja el grafo igual. El FOREACH es el truco de Cypher para un "if": solo crea la categoría si no es null.
     */
    private static final String EVENT_CREATED_CYPHER = """
            MERGE (e:Event {id: $id})
            SET e.visibility = $visibility,
                e.startsAt   = datetime($startsAt),
                e.location   = CASE WHEN $lat IS NULL OR $lon IS NULL THEN null
                                    ELSE point({latitude: $lat, longitude: $lon}) END
            MERGE (u:User {id: $ownerId})
            MERGE (u)-[:CREATED]->(e)
            FOREACH (_ IN CASE WHEN $categoryId IS NULL THEN [] ELSE [1] END |
                MERGE (c:Category {id: $categoryId})
                MERGE (e)-[:IN_CATEGORY]->(c))
            """;

    /**
     * Neo4jClient y no UserNeoRepository: los repositorios de Spring Data son @Transactional y, al llamarlos desde
     * ProcessGraphOutboxService, se unirían a su transacción de PostgreSQL. Una excepción de Neo4j la marcaría como
     * rollback-only y se perdería el lote entero (incluido el markFailed). Neo4jClient ejecuta en auto-commit.
     */
    private final Neo4jClient neo4j;

    @Override
    public boolean apply(OutboxEvent event) {
        return switch (event.getEventType()) {
            case USER_REGISTERED -> applyUserRegistered(event);
            case EVENT_CREATED -> applyEventCreated(event);
            case FRIEND_ADDED -> {
                log.warn("Evento de grafo {} ({}) aún sin implementar: se queda pendiente", event.getId(),
                        event.getEventType());
                yield false;
            }
        };
    }

    @Override
    public int ensureUsers(List<UUID> userIds) {
        if (userIds.isEmpty()) {
            return 0;
        }
        List<String> ids = userIds.stream().map(UUID::toString).toList();
        var summary = neo4j(() -> neo4j.query("UNWIND $ids AS id MERGE (:User {id: id})")
                .bind(ids)
                .to("ids")
                .run());
        return summary.counters().nodesCreated();
    }

    @Override
    public int ensureEvents(List<UUID> eventIds) {
        if (eventIds.isEmpty()) {
            return 0;
        }
        List<String> ids = eventIds.stream().map(UUID::toString).toList();
        var summary = neo4j(() -> neo4j.query("UNWIND $ids AS id MERGE (:Event {id: id})")
                .bind(ids)
                .to("ids")
                .run());
        return summary.counters().nodesCreated();
    }

    private boolean applyUserRegistered(OutboxEvent event) {
        // Payload roto = error permanente: sale como excepción normal y gasta un intento
        var payload = JSON.readValue(event.getPayload(), UserPayload.class);
        if (payload.userId() == null) {
            throw new IllegalArgumentException("Payload sin userId en el evento " + event.getId());
        }
        neo4j(() -> neo4j.query("MERGE (:User {id: $id})").bind(payload.userId().toString()).to("id").run());
        return true;
    }

    private boolean applyEventCreated(OutboxEvent event) {
        // Payload roto = error permanente: sale como excepción normal y gasta un intento
        var payload = JSON.readValue(event.getPayload(), EventPayload.class);
        if (payload.eventId() == null || payload.ownerId() == null || payload.startsAt() == null
                || payload.visibility() == null) {
            throw new IllegalArgumentException("Payload incompleto en el evento " + event.getId());
        }

        // HashMap y no Map.of: Map.of no admite null y lat/lon/categoryId pueden serlo
        var params = new HashMap<String, Object>();
        params.put("id", payload.eventId().toString());
        params.put("ownerId", payload.ownerId().toString());
        params.put("visibility", payload.visibility().toUpperCase(Locale.ROOT)); // "public" → "PUBLIC" (enum de EventNode)
        params.put("startsAt", payload.startsAt().toString()); // ISO-8601 → datetime() en Cypher
        params.put("lat", toDouble(payload.lat())); // el driver no acepta BigDecimal
        params.put("lon", toDouble(payload.lon()));
        params.put("categoryId", payload.categoryId() == null ? null : payload.categoryId().longValue());

        neo4j(() -> neo4j.query(EVENT_CREATED_CYPHER).bindAll(params).run());
        return true;
    }

    private static Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    /** Ejecuta una operación en Neo4j traduciendo los fallos de disponibilidad. */
    private <T> T neo4j(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (RuntimeException e) {
            if (isUnavailable(e)) {
                throw new GraphUnavailableException("Neo4j no disponible: " + e.getMessage(), e);
            }
            throw e;
        }
    }

    /**
     * Neo4jClient a veces devuelve la excepción del driver y a veces la traduce a una DataAccessException de Spring: se
     * revisa toda la cadena de causas.
     */
    private static boolean isUnavailable(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof ServiceUnavailableException
                    || t instanceof SessionExpiredException
                    || t instanceof TransientException
                    || t instanceof TransientDataAccessException
                    || t instanceof RecoverableDataAccessException
                    || t instanceof DataAccessResourceFailureException) {
                return true;
            }
        }
        return false;
    }

    /** {"userId": "..."} — lo escribe ActivateAccountService. */
    private record UserPayload(UUID userId) {
    }

    /** {"eventId": "...", "ownerId": "...", ...} — lo escribe CreateEventService. */
    private record EventPayload(
            UUID eventId,
            UUID ownerId,
            Short categoryId,
            String visibility,
            Instant startsAt,
            BigDecimal lat,
            BigDecimal lon) {
    }
}
