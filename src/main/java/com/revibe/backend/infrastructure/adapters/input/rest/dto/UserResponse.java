package com.revibe.backend.infrastructure.adapters.input.rest.dto;

import com.revibe.backend.domain.model.User;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * Lo que devolvemos al cliente. Nunca password_hash.
 */
public record UserResponse(UUID id, String username, String email, String firstName, String lastName, String status,
        Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getFirstName(),
                user.getLastName(), user.getStatus().name().toLowerCase(Locale.ROOT), user.getCreatedAt());
    }
}
