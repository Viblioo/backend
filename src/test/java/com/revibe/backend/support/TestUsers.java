package com.revibe.backend.support;

import com.revibe.backend.domain.model.User;
import com.revibe.backend.domain.model.UserStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Usuarios de dominio para tests unitarios. */
public final class TestUsers {

    public static final String PASSWORD_HASH = "stored-hash";

    private TestUsers() {
    }

    public static User user(UserStatus status) {
        return new User(UUID.randomUUID(), "maria", "maria@example.com", PASSWORD_HASH, "María", null,
                LocalDate.of(2000, 5, 14), status, Instant.parse("2026-01-01T10:00:00Z"));
    }
}
