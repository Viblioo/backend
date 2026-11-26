package com.revibe.backend.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Activa los @Scheduled (outbox del grafo, reconciliación). Con revibe.scheduling.enabled=false no se ejecuta ninguno:
 * lo usan los tests para que no procesen datos de la BD de desarrollo mientras corren.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "revibe.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
