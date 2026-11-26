package com.revibe.backend.infrastructure.adapters.output.persistence.postgres.repository;

import com.revibe.backend.domain.model.UserStatus;
import com.revibe.backend.infrastructure.adapters.output.persistence.postgres.entity.UserEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByEmail(String email);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update UserEntity u set u.passwordHash = :hash where u.id = :id")
    int updatePasswordHash(@Param("id")
    UUID id, @Param("hash")
    String passwordHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update UserEntity u
               set u.status = :to, u.emailVerifiedAt = :verifiedAt
             where u.id = :id and u.status = :from
            """)
    int updateStatus(@Param("id")
    UUID id,
            @Param("from")
            UserStatus from,
            @Param("to")
            UserStatus to,
            @Param("verifiedAt")
            Instant verifiedAt);

    /** Nativa: el orden de UUID que importa es el de PostgreSQL, que es el que usa el "id > :after". */
    @Query(value = """
            SELECT id FROM users
             WHERE status = 'active' AND id > :after
             ORDER BY id
             LIMIT :limit
            """, nativeQuery = true)
    List<UUID> findActiveIdsAfter(@Param("after")
    UUID after, @Param("limit")
    int limit);
}
