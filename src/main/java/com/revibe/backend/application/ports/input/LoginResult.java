package com.revibe.backend.application.ports.input;

import com.revibe.backend.domain.model.User;
import java.time.Duration;

/**
 * @param expiresIn
 *            caducidad de la sesión si no se usa (cada petición con el token la renueva)
 */
public record LoginResult(User user, String token, Duration expiresIn) {

    // Que el token no acabe en los logs
    @Override
    public String toString() {
        return "LoginResult[userId=%s]".formatted(user.getId());
    }
}
