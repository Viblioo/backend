package com.revibe.backend.infrastructure.adapters.output.persistence.postgres;

import com.revibe.backend.application.ports.output.user.UserRepositoryPort;
import com.revibe.backend.domain.exception.UserAlreadyExistsException;
import com.revibe.backend.domain.model.User;
import com.revibe.backend.domain.model.UserStatus;
import com.revibe.backend.infrastructure.adapters.output.persistence.postgres.mapper.UserPersistenceMapper;
import com.revibe.backend.infrastructure.adapters.output.persistence.postgres.repository.UserJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserRepositoryPort {

    // Nombres que PostgreSQL da a las UNIQUE de V1 (tabla_columna_key)
    private static final String UQ_USERNAME = "users_username_key";
    private static final String UQ_EMAIL = "users_email_key";

    private final UserJpaRepository jpaRepository;
    private final UserPersistenceMapper mapper;

    @Override
    public boolean existsByUsername(String username) {
        return jpaRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaRepository.findByUsername(username).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public void updatePasswordHash(UUID userId, String passwordHash) {
        jpaRepository.updatePasswordHash(userId, passwordHash);
    }

    @Override
    public User save(User user) {
        try {
            return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(user)));
        } catch (DataIntegrityViolationException ex) {
            throw translate(ex);
        }
    }

    @Override
    public boolean activate(UUID userId, Instant verifiedAt) {
        return jpaRepository.updateStatus(userId, UserStatus.PENDING, UserStatus.ACTIVE, verifiedAt) == 1;
    }

    @Override
    public List<UUID> findActiveIdsAfter(UUID afterId, int limit) {
        return jpaRepository.findActiveIdsAfter(afterId, limit);
    }

    //transofrm database exceptions (500) to business logic exceptions (409)
    private RuntimeException translate(DataIntegrityViolationException ex) {
        if (ex.getCause() instanceof ConstraintViolationException cve && cve.getConstraintName() != null) {
            String constraint = cve.getConstraintName().toLowerCase(Locale.ROOT);
            if (constraint.contains(UQ_USERNAME)) {
                return new UserAlreadyExistsException(UserAlreadyExistsException.Field.USERNAME);
            }
            if (constraint.contains(UQ_EMAIL)) {
                return new UserAlreadyExistsException(UserAlreadyExistsException.Field.EMAIL);
            }
        }
        return ex; // otra violación (CHECK, NOT NULL...): es un bug nuestro, que salga como 500
    }
}
