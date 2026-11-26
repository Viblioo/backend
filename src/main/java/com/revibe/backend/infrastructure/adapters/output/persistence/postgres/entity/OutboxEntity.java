package com.revibe.backend.infrastructure.adapters.output.persistence.postgres.entity;

import com.revibe.backend.domain.model.OutboxEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Mapeo JPA de graph_outbox (V8__graph_outbox.sql). Cada fila es un cambio pendiente de replicar en Neo4j: se inserta
 * en la misma transacción que el cambio en PostgreSQL y un proceso aparte la aplica y rellena processed_at.
 */
@Entity
@Table(name = "graph_outbox")
@Getter
@Setter
@NoArgsConstructor
public class OutboxEntity {

    /** BIGSERIAL: también da el orden de llegada en que hay que procesar los eventos. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Se guarda el nombre del enum (FRIEND_ADDED...), como en la columna. */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private OutboxEventType eventType;

    /** JSON en texto; Hibernate lo escribe como jsonb. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** NULL = pendiente. */
    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

}
