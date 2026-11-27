package com.revibe.backend.infrastructure.adapters.output.persistence.neo4j;

import com.revibe.backend.application.ports.output.outbox.GraphSyncPort;
import com.revibe.backend.application.ports.output.outbox.GraphUnavailableException;
import com.revibe.backend.domain.model.OutboxEvent;
import java.util.List;
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
     * Neo4jClient y no UserNeoRepository: los repositorios de Spring Data son @Transactional y, al llamarlos desde
     * ProcessGraphOutboxService, se unirían a su transacción de PostgreSQL. Una excepción de Neo4j la marcaría como
     * rollback-only y se perdería el lote entero (incluido el markFailed). Neo4jClient ejecuta en auto-commit.
     */
    private final Neo4jClient neo4j;

    @Override
    public boolean apply(OutboxEvent event) {
        return switch (event.getEventType()) {
            case USER_REGISTERED -> applyUserRegistered(event);
            case FRIEND_ADDED, EVENT_CREATED -> {
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

    private boolean applyUserRegistered(OutboxEvent event) {
        // Payload roto = error permanente: sale como excepción normal y gasta un intento
        var payload = JSON.readValue(event.getPayload(), UserPayload.class);
        if (payload.userId() == null) {
            throw new IllegalArgumentException("Payload sin userId en el evento " + event.getId());
        }
        neo4j(() -> neo4j.query("MERGE (:User {id: $id})").bind(payload.userId().toString()).to("id").run());
        return true;
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
}
