package com.revibe.backend.domain.model;

import com.revibe.backend.domain.exception.InvalidUserDataException;
import java.nio.charset.StandardCharsets;

/**
 * Reglas de una contraseña nueva (registro, recuperación, cambio). Un único sitio para que no se desincronicen.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;

    /** BCrypt solo usa los primeros 72 bytes: más se rechaza (y una tilde o un emoji ocupan más de un byte). */
    public static final int MAX_BYTES = 72;

    private PasswordPolicy() {
    }

    /**
     * @param field
     *            nombre del campo para el error que verá la app
     */
    public static void validate(String rawPassword, String field) {
        if (rawPassword == null || rawPassword.length() < MIN_LENGTH) {
            throw new InvalidUserDataException(field, "Mínimo " + MIN_LENGTH + " caracteres");
        }
        if (exceedsMaxBytes(rawPassword)) {
            throw new InvalidUserDataException(field, "La contraseña es demasiado larga");
        }
    }

    /** Una contraseña así no puede coincidir con ningún hash guardado. */
    public static boolean exceedsMaxBytes(String rawPassword) {
        return rawPassword.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES;
    }
}
