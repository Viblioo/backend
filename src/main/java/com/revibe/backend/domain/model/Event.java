package com.revibe.backend.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Event {

    private UUID id;
    private UUID ownerId;
    private String title;
    private String description;
    private short categoryId; // int2
    private String placeName;
    private BigDecimal lat; // numeric(9,6)
    private BigDecimal lon; // numeric(9,6)
    private Instant startsAt; // timestamptz
    private Instant endsAt;
    private EventVisibility visibility; // 'private' | 'friends' | 'public'
    private Integer maxParticipants; // int4
    private String coverKey;
    private String currency; // char(3), p. ej. "EUR"
    private EventStatus status; // 'active' | 'cancelled'
    private Instant createdAt;
    private Instant updatedAt;
}
