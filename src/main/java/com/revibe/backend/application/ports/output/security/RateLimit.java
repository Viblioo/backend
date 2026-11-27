package com.revibe.backend.application.ports.output.security;

import java.time.Duration;

/**
 * Límites de peticiones/intentos. Es política de la aplicación (cuántos y cada cuánto); cómo se cuentan lo decide el
 * adaptador. La ventana empieza con la primera petición contada y dura {@code window}.
 */
public enum RateLimit {

    /** Logins fallidos contra una misma cuenta (o username/email inexistente). */
    LOGIN_FAILURES_PER_ACCOUNT(5, Duration.ofMinutes(15)),

    /** Logins fallidos desde una IP, contra cualquier cuenta: frena probar una contraseña contra muchos usuarios. */
    LOGIN_FAILURES_PER_IP(30, Duration.ofMinutes(15)),

    /** Contraseña actual incorrecta al cambiarla con la sesión iniciada (por si roban la sesión). */
    CHANGE_PASSWORD_FAILURES_PER_USER(5, Duration.ofMinutes(15)),

    /** Registros desde una IP. */
    REGISTER_PER_IP(10, Duration.ofHours(1)),

    /** Peticiones que envían un email (reenviar activación, recuperar contraseña) desde una IP. */
    EMAIL_REQUESTS_PER_IP(10, Duration.ofHours(1)),

    /** Peticiones que envían un email a una misma dirección: que no se pueda usar para hacer spam a alguien. */
    EMAIL_REQUESTS_PER_ADDRESS(3, Duration.ofHours(1));

    private final int maxAttempts;
    private final Duration window;

    RateLimit(int maxAttempts, Duration window) {
        this.maxAttempts = maxAttempts;
        this.window = window;
    }

    public int maxAttempts() {
        return maxAttempts;
    }

    public Duration window() {
        return window;
    }
}
