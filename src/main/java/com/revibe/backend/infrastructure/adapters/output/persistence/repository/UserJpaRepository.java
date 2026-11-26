package com.revibe.backend.infrastructure.adapters.output.persistence.repository;

import com.revibe.backend.domain.model.UserStatus;
import com.revibe.backend.infrastructure.adapters.output.persistence.entity.UserEntity;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update UserEntity u
               set u.status = :to, u.emailVerifiedAt = :verifiedAt
             where u.id = :id and u.status = :from
            """)
    int updateStatus(@Param("id") UUID id,
            @Param("from") UserStatus from,
            @Param("to") UserStatus to,
            @Param("verifiedAt") Instant verifiedAt);
}
