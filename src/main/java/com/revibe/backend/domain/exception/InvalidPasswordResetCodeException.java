package com.revibe.backend.domain.exception;

/**
 * Código de recuperación incorrecto, caducado, agotado, o email sin código. Mismo mensaje en todos los casos: no debe
 * revelar si el email existe.
 */
public class InvalidPasswordResetCodeException extends RuntimeException {

    public InvalidPasswordResetCodeException() {
        super("El código no es válido o ha caducado");
    }
}
