package com.revibe.backend.application.ports.input.event;

import com.revibe.backend.domain.model.EventVisibility;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Datos de entrada del caso de uso de crear evento. No sabe nada de HTTP: el controlador lo construye a partir de
 * CreateEventRequest y del usuario autenticado.
 */
public record CreateEventCommand(
        UUID ownerId,
        String title,
        String description,
        short categoryId,
        String placeName,
        BigDecimal lat,
        BigDecimal lon,
        Instant startsAt,
        Instant endsAt,
        EventVisibility visibility, // null → el servicio pone PUBLIC
        Integer maxParticipants,
        String coverKey,
        String currency // viene validada del request
) {
}
