package com.revibe.backend.application.ports.output;

import com.revibe.backend.domain.model.OutboxEvent;
import java.util.List;
import java.util.UUID;

/**
 * Escritura en el grafo (Neo4j). Todas las operaciones son idempotentes.
 * <p>
 * Si el grafo no está disponible lanzan {@link GraphUnavailableException}; cualquier otra excepción significa que el
 * problema es del dato (payload roto...) y reintentar no lo va a arreglar.
 */
public interface GraphSyncPort {

    /**
     * Aplica un evento del outbox.
     *
     * @return true si se ha aplicado; false si es un tipo de evento que todavía no se sabe aplicar (se queda pendiente)
     */
    boolean apply(OutboxEvent event);

    /**
     * Se asegura de que existe un nodo User por cada id.
     *
     * @return cuántos nodos faltaban y se han creado
     */
    int ensureUsers(List<UUID> userIds);
}
