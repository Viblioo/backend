package com.revibe.backend.domain.model;

/**
 * Tipos de cambio que se replican en el grafo. Se guardan tal cual (FRIEND_ADDED...) en graph_outbox.event_type, que es
 * VARCHAR(40): ningún nombre puede pasar de 40 caracteres.
 */
public enum OutboxEventType {
    FRIEND_ADDED, EVENT_CREATED, USER_REGISTERED
}
