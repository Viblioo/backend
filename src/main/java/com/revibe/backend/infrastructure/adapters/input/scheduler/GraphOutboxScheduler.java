package com.revibe.backend.infrastructure.adapters.input.scheduler;

import com.revibe.backend.application.ports.input.outbox.ProcessGraphOutboxUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Procesa graph_outbox cada pocos segundos. fixedDelay: la siguiente pasada empieza cuando termina la anterior, así que
 * nunca se solapan dentro de la misma instancia.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GraphOutboxScheduler {

    private final ProcessGraphOutboxUseCase processGraphOutbox;

    @Scheduled(fixedDelayString = "${revibe.graph-outbox.poll-delay-ms:5000}", initialDelayString = "${revibe.graph-outbox.initial-delay-ms:10000}")
    public void run() {
        try {
            var result = processGraphOutbox.processPending();
            if (result.total() > 0) {
                log.info("graph_outbox: {} procesados, {} fallidos", result.processed(), result.failed());
            }
        } catch (RuntimeException e) {
            // Si cae PostgreSQL no queremos un stacktrace cada 5 s sin contexto
            log.error("No se pudo procesar graph_outbox", e);
        }
    }
}
