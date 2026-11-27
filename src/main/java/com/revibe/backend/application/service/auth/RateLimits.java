package com.revibe.backend.application.service.auth;

import com.revibe.backend.application.ports.output.security.RateLimit;
import com.revibe.backend.application.ports.output.security.RateLimiterPort;
import com.revibe.backend.domain.exception.TooManyRequestsException;

/** Atajos para lanzar TooManyRequestsException desde los servicios. */
final class RateLimits {

    private RateLimits() {
    }

    /** Cuenta la petición y lanza 429 si con ella se pasa del límite. */
    static void acquire(RateLimiterPort limiter, RateLimit limit, String key) {
        limiter.tryAcquire(limit, key).ifPresent(wait -> {
            throw new TooManyRequestsException(wait);
        });
    }

    /** Lanza 429 si ya se ha llegado al límite, sin contar esta petición. */
    static void checkNotBlocked(RateLimiterPort limiter, RateLimit limit, String key) {
        limiter.blockedFor(limit, key).ifPresent(wait -> {
            throw new TooManyRequestsException(wait);
        });
    }
}
