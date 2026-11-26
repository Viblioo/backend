package com.revibe.backend.infrastructure.adapters.output.persistence.postgres.repository;

import com.revibe.backend.infrastructure.adapters.output.persistence.postgres.entity.OutboxEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxJpaRepository extends JpaRepository<OutboxEntity, Long> {
    @Query(value = """
            SELECT *
            FROM graph_outbox
            WHERE processed_at IS NULL
              AND attempts < :maxAttempts
            ORDER BY id
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEntity> findPending(@Param("limit")
    int limit, @Param("maxAttempts")
    int maxAttempts);
}
