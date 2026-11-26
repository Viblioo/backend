package com.revibe.backend.application.ports.input;

import java.util.UUID;

/**
 * @param currentToken
 *            token de la sesión desde la que se cambia, que se mantiene abierta
 */
public record ChangePasswordCommand(UUID userId, String currentToken, String currentPassword, String newPassword) {

    @Override
    public String toString() {
        return "ChangePasswordCommand[userId=%s]".formatted(userId);
    }
}
