package com.revibe.backend.domain.exception;

/**
 * Usuario o contraseña incorrectos. El mismo mensaje en ambos casos: no se debe poder averiguar qué usernames existen.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Usuario o contraseña incorrectos");
    }
}
