package com.revibe.backend.infrastructure.adapters.input.rest.security;

import java.util.Optional;

/** Saca el token de una cabecera "Authorization: Bearer &lt;token&gt;". La usan el filtro y el logout. */
public final class BearerToken {

    private static final String PREFIX = "Bearer ";

    private BearerToken() {
    }

    public static Optional<String> from(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(PREFIX)) {
            return Optional.empty();
        }
        String token = authorizationHeader.substring(PREFIX.length()).strip();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }
}
