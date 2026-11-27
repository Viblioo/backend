package com.revibe.backend.infrastructure.adapters.output.persistence.postgres.repository;

import com.revibe.backend.infrastructure.adapters.output.persistence.postgres.entity.EventEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventJpaRepository extends JpaRepository<EventEntity, UUID> {

    /** Nativa: el orden de UUID que importa es el de PostgreSQL, que es el que usa el "id > :after". */
    @Query(value = """
            SELECT id FROM event
             WHERE id > :after
             ORDER BY id
             LIMIT :limit
            """, nativeQuery = true)
    List<UUID> findIdsAfter(@Param("after")
    UUID after, @Param("limit")
    int limit);
}
