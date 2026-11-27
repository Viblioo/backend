package com.revibe.backend.infrastructure.adapters.input.rest.dto;

import com.revibe.backend.domain.model.Event;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * Lo que devolvemos al cliente al crear un evento. La portada se devuelve como URL, nunca la coverKey en bruto.
 */
public record CreateEventResponse(
        UUID id,
        UUID ownerId,
        String title,
        String description,
        short categoryId,
        String placeName,
        BigDecimal lat,
        BigDecimal lon,
        Instant startsAt,
        Instant endsAt,
        String visibility,
        Integer maxParticipants,
        String coverUrl,
        String currency,
        String status,
        Instant createdAt,
        Instant updatedAt) {

    public static CreateEventResponse from(Event event, String coverUrl) {
        return new CreateEventResponse(
                event.getId(),
                event.getOwnerId(),
                event.getTitle(),
                event.getDescription(),
                event.getCategoryId(),
                event.getPlaceName(),
                event.getLat(),
                event.getLon(),
                event.getStartsAt(),
                event.getEndsAt(),
                event.getVisibility().name().toLowerCase(Locale.ROOT),
                event.getMaxParticipants(),
                coverUrl,
                event.getCurrency(),
                event.getStatus().name().toLowerCase(Locale.ROOT),
                event.getCreatedAt(),
                event.getUpdatedAt());
    }
}
