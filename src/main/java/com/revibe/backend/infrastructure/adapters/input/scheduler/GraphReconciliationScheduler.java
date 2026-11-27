package com.revibe.backend.infrastructure.adapters.input.scheduler;

import com.revibe.backend.application.ports.input.outbox.ReconcileGraphUseCase;
import com.revibe.backend.application.ports.output.outbox.GraphUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Reconciliación nocturna PostgreSQL -> Neo4j. Por defecto a las 4:00; se cambia con revibe.graph-reconciliation.cron.
 * Si hay varias instancias se ejecuta en todas, pero da igual: es idempotente.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GraphReconciliationScheduler {

    private final ReconcileGraphUseCase reconcileGraph;

    @Scheduled(cron = "${revibe.graph-reconciliation.cron:0 0 4 * * *}")
    public void run() {
        try {
            int created = reconcileGraph.reconcileUsers();
            if (created > 0) {
                // No debería pasar: si pasa, el outbox está perdiendo eventos y hay que mirar por qué
                log.warn("Reconciliación del grafo: se han creado {} nodos User que faltaban", created);
            } else {
                log.info("Reconciliación del grafo: todo cuadra");
            }
        } catch (GraphUnavailableException e) {
            log.warn("Reconciliación del grafo saltada, Neo4j no disponible: {}", e.getMessage());
        } catch (RuntimeException e) {
            log.error("Reconciliación del grafo fallida", e);
        }
    }
}
