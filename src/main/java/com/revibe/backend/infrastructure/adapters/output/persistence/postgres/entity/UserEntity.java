package com.revibe.backend.infrastructure.adapters.output.persistence.postgres.entity;

import com.revibe.backend.domain.model.UserStatus;
import com.revibe.backend.infrastructure.adapters.output.persistence.postgres.converter.UserStatusConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Mapeo JPA de la tabla users (V1__extensions_and_users.sql). Solo se mapean las columnas que usa el registro; el resto
 * tienen DEFAULT o admiten NULL.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, columnDefinition = "citext")
    private String username;

    @Column(nullable = false, columnDefinition = "citext")
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", length = 80)
    private String lastName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Convert(converter = UserStatusConverter.class)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // updated_at lo mantiene el trigger trg_users_updated_at.
    // TODO: is_private (y avatar_key, bio, deleted_at cuando hagan falta)
}
