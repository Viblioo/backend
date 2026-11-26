package com.revibe.backend.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Activa @Async (envío de emails). Usa el executor que autoconfigura Spring Boot. */
@Configuration
@EnableAsync
public class AsyncConfig {
}
