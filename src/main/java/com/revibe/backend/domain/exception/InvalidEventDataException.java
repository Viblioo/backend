package com.revibe.backend.domain.exception;

/**
 * Datos de un evento que no cumplen las reglas de negocio. Mismo formato que InvalidUserDataException: el campo que
 * falla y el mensaje para el usuario.
 */
public class InvalidEventDataException extends RuntimeException {

    private final String field;

    public InvalidEventDataException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
