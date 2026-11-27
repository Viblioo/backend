package com.revibe.backend.infrastructure.adapters.output.persistence.postgres.entity;

import com.revibe.backend.domain.model.EventStatus;
import com.revibe.backend.domain.model.EventVisibility;
import com.revibe.backend.infrastructure.adapters.output.persistence.postgres.converter.EventStatusConverter;
import com.revibe.backend.infrastructure.adapters.output.persistence.postgres.converter.EventVisibilityConverter;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "event")
@Getter
@Setter
@NoArgsConstructor
public class EventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "category_id", nullable = false)
    private short categoryId; // int2

    @Column(name = "place_name", length = 150)
    private String placeName;

    @Column(precision = 9, scale = 6)
    private BigDecimal lat; // numeric(9,6)

    @Column(precision = 9, scale = 6)
    private BigDecimal lon; // numeric(9,6)

    @Column(name = "starts_at")
    private Instant startsAt; // timestamptz

    @Column(name = "ends_at")
    private Instant endsAt;

    @Convert(converter = EventVisibilityConverter.class)
    @Column(length = 10)
    private EventVisibility visibility;

    @Column(name = "max_participants")
    private Integer maxParticipants; // int4

    @Column(name = "cover_key", columnDefinition = "text")
    private String coverKey;

    @Column(columnDefinition = "bpchar(3)")
    private String currency; // char(3), p. ej. "EUR"

    @Convert(converter = EventStatusConverter.class)
    @Column(length = 10)
    private EventStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
