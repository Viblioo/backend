package com.revibe.backend.application.ports.output.security;

import java.time.Duration;
import java.util.Optional;

/**
 * Contadores con ventana de tiempo. Dos formas de uso:
 * <ul>
 * <li>Solo fallos (login): {@link #blockedFor} antes de intentarlo, {@link #recordFailure} si falla y {@link #reset} si
 * sale bien.</li>
 * <li>Toda petición cuenta (registro, emails): {@link #tryAcquire}.</li>
 * </ul>
 */
public interface RateLimiterPort {

    /** Si {@code key} ya ha llegado al máximo, cuánto falta para que se libere. Vacío si puede seguir. */
    Optional<Duration> blockedFor(RateLimit limit, String key);

    /** Suma un intento a {@code key}. */
    void recordFailure(RateLimit limit, String key);

    /** Pone el contador de {@code key} a cero. */
    void reset(RateLimit limit, String key);

    /**
     * Cuenta esta petición. Si con ella se pasa del máximo devuelve cuánto falta para que se libere; si no, vacío.
     */
    Optional<Duration> tryAcquire(RateLimit limit, String key);
}
