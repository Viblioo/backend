package com.revibe.backend.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Usuario del dominio. No sabe nada de JPA, Spring ni HTTP.
 */
@Getter
@AllArgsConstructor
public class User {

    private final UUID id;
    private final String username;
    private final String email;
    private final String passwordHash;
    private final String firstName;
    private final String lastName;
    private final LocalDate birthDate;
    private final UserStatus status;
    private final Instant createdAt;
}
