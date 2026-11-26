package com.revibe.backend.domain.exception;

import lombok.Getter;

@Getter
public class InvalidUserDataException extends RuntimeException {

    private final String field;

    public InvalidUserDataException(String field, String message) {
        super(message);
        this.field = field;
    }
}
