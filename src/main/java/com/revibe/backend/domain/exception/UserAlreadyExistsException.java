package com.revibe.backend.domain.exception;

import lombok.Getter;

@Getter
public class UserAlreadyExistsException extends RuntimeException {

    public enum Field {
        USERNAME, EMAIL
    }

    private final Field field;

    public UserAlreadyExistsException(Field field) {
        super(field == Field.USERNAME ? "El nombre de usuario ya está en uso" : "El email ya está registrado");
        this.field = field;
    }
}
