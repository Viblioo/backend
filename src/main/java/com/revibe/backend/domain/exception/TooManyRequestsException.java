package com.revibe.backend.domain.exception;

import java.time.Duration;
import lombok.Getter;

/** Se ha superado un límite de peticiones o intentos: hay que esperar {@code retryAfter}. */
@Getter
public class TooManyRequestsException extends RuntimeException {

    private final Duration retryAfter;

    public TooManyRequestsException(Duration retryAfter) {
        super("Demasiados intentos. Vuelve a intentarlo en %d minutos"
                .formatted(Math.max(1, (retryAfter.toSeconds() + 59) / 60)));
        this.retryAfter = retryAfter;
    }
}
