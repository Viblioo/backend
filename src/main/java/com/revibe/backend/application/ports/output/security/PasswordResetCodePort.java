package com.revibe.backend.application.ports.output.security;

import java.util.UUID;

/**
 * Códigos numéricos de recuperación de contraseña: uno activo por usuario, de un solo uso, con caducidad y con un
 * máximo de intentos (un código de 6 cifras sin límite de intentos se adivinaría probando).
 */
public interface PasswordResetCodePort {

    /** Genera un código nuevo (invalida el anterior del mismo usuario) y lo devuelve en claro. */
    String create(UUID userId);

    /**
     * Comprueba el código. Si es correcto lo consume y devuelve true. Si no, suma un intento fallido; al llegar al
     * máximo el código se invalida aunque luego se acierte.
     */
    boolean verify(UUID userId, String code);
}
