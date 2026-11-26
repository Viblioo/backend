package com.revibe.backend.infrastructure.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    /** Inyectar el reloj permite fijar "hoy" en los tests (p. ej. para la edad mínima). */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
