package com.revibe.backend.infrastructure.adapters.output.persistence.repository;

import com.revibe.backend.infrastructure.adapters.output.persistence.entity.UserEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
