package com.revibe.backend.application.service;

import com.revibe.backend.application.ports.input.OutboxBatchResult;
import com.revibe.backend.application.ports.input.ProcessGraphOutboxUseCase;
import com.revibe.backend.application.ports.output.GraphOutboxPort;
import com.revibe.backend.application.ports.output.GraphSyncPort;
import com.revibe.backend.application.ports.output.GraphUnavailableException;
import com.revibe.backend.domain.model.OutboxEvent;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lee un lote de graph_outbox, lo aplica en Neo4j y marca cada evento en PostgreSQL.
 * <p>
 * Todo el lote va en una transacción de PostgreSQL: findPending usa FOR UPDATE SKIP LOCKED, así que las filas quedan
 * bloqueadas hasta el commit y otra instancia de la app no puede coger las mismas. Los cambios de markProcessed /
 * markFailed se escriben en ese commit.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessGraphOutboxService implements ProcessGraphOutboxUseCase {

    static final int BATCH_SIZE = 50;
    static final int MAX_ATTEMPTS = 10;
    private static final int MAX_ERROR_LENGTH = 1000;

    private final GraphOutboxPort outbox;
    private final GraphSyncPort graph;
    private final Clock clock;

    @Override
    @Transactional
    public OutboxBatchResult processPending() {
        int processed = 0;
        int failed = 0;

        for (OutboxEvent event : outbox.findPending(BATCH_SIZE, MAX_ATTEMPTS)) {
            try {
                if (graph.apply(event)) {
                    event.markProcessed(Instant.now(clock));
                    processed++;
                }
            } catch (GraphUnavailableException e) {
                // Neo4j caído: no es culpa del evento, no gastamos intentos. Se corta el lote (el resto fallaría
                // igual) y se reintenta en la siguiente pasada. Lo ya procesado en este lote sí se guarda.
                log.warn("Neo4j no disponible, se reintentará en la siguiente pasada: {}", e.getMessage());
                break;
            } catch (RuntimeException e) {
                // Un evento roto no debe parar el resto del lote
                event.markFailed(describe(e));
                failed++;
                if (event.getAttempts() >= MAX_ATTEMPTS) {
                    log.error("Evento de grafo {} ({}) descartado tras {} intentos: {}", event.getId(),
                            event.getEventType(), event.getAttempts(), event.getLastError());
                } else {
                    log.warn("Evento de grafo {} ({}) ha fallado (intento {}): {}", event.getId(),
                            event.getEventType(), event.getAttempts(), event.getLastError());
                }
            }
            outbox.save(event);
        }

        return new OutboxBatchResult(processed, failed);
    }

    private static String describe(RuntimeException e) {
        String message = e.getClass().getSimpleName() + ": " + e.getMessage();
        return message.length() > MAX_ERROR_LENGTH ? message.substring(0, MAX_ERROR_LENGTH) : message;
    }
}
