package com.revibe.backend.application.event;

/** Hay que enviar un código de recuperación de contraseña. */
public record PasswordResetRequestedEvent(String email, String code) {

    // El código permite cambiar la contraseña: que no salga en los logs
    @Override
    public String toString() {
        return "PasswordResetRequestedEvent[email=%s]".formatted(email);
    }
}
