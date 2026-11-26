package com.revibe.backend.domain.exception;

public class InvalidActivationCodeException extends RuntimeException {

    public InvalidActivationCodeException() {
        super("El código de activación no es válido o ha caducado");
    }
}
