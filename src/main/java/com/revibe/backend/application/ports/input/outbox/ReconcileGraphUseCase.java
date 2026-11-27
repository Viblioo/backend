package com.revibe.backend.application.ports.input.outbox;

public interface ReconcileGraphUseCase {

    /**
     * Crea en el grafo los nodos User que falten para los usuarios ACTIVE de PostgreSQL.
     *
     * @return cuántos nodos faltaban (si no es 0, algo se le ha escapado al outbox)
     */
    int reconcileUsers();

    int reconcileEvents();
}
