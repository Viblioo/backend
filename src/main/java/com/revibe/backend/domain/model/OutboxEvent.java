package com.revibe.backend.domain.model;

import java.time.Instant;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Cambio pendiente de replicar en el grafo (Neo4j). Se guarda en la misma transacción que el cambio en PostgreSQL y un
 * proceso aparte lo aplica después. No sabe nada de JPA, Spring ni Neo4j.
 */
@Getter
@AllArgsConstructor
public class OutboxEvent {

    private final Long id;
    private final OutboxEventType eventType;
    private final String payload;
    private final Instant createdAt;
    private Instant processedAt;
    private int attempts;
    private String lastError;

    /** Evento nuevo, aún sin id ni createdAt (los pone la BD al guardarlo). */
    public static OutboxEvent pending(OutboxEventType eventType, String payload) {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(payload, "payload");
        return new OutboxEvent(null, eventType, payload, null, null, 0, null);
    }

    public boolean isPending() {
        return processedAt == null;
    }

    /** Aplicado en el grafo: deja de estar pendiente. */
    public void markProcessed(Instant now) {
        this.processedAt = Objects.requireNonNull(now, "now");
        this.lastError = null;
    }

    /** Ha fallado al aplicarlo: sigue pendiente para reintentarlo y guarda el motivo. */
    public void markFailed(String error) {
        this.attempts++;
        this.lastError = error;
    }
}
