package com.revibe.backend.application.service.event;

import com.revibe.backend.application.ports.input.event.CreateEventCommand;
import com.revibe.backend.application.ports.input.event.CreateEventResult;
import com.revibe.backend.application.ports.input.event.CreateEventUseCase;
import com.revibe.backend.application.ports.output.event.EventRepositoryPort;
import com.revibe.backend.application.ports.output.outbox.GraphOutboxPort;
import com.revibe.backend.domain.exception.InvalidEventDataException;
import com.revibe.backend.domain.model.*;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateEventService implements CreateEventUseCase {

    /** Visibilidad si el usuario no elige ninguna. */
    private static final EventVisibility DEFAULT_VISIBILITY = EventVisibility.PRIVATE;

    private final EventRepositoryPort eventRepository;
    private final GraphOutboxPort outboxRepository;
    private final Clock clock;

    @Override
    @Transactional
    public CreateEventResult create(CreateEventCommand command) {

        if (command.startsAt().isBefore(Instant.now(clock))) {
            throw new InvalidEventDataException("startsAt", "El evento no puede empezar en el pasado");
        }

        if (!command.endsAt().isAfter(command.startsAt())) {
            throw new InvalidEventDataException("endsAt", "La fecha de fin debe ser posterior a la de inicio");
        }

        if (command.maxParticipants() != null && command.maxParticipants() <= 0) {
            throw new InvalidEventDataException("maxParticipants", "Debe ser mayor que 0");
        }

        var event = new Event(
                null, // id: lo genera Hibernate
                command.ownerId(),
                command.title().strip(),
                blankToNull(command.description()),
                command.categoryId(),
                blankToNull(command.placeName()),
                command.lat(),
                command.lon(),
                command.startsAt(),
                command.endsAt(),
                command.visibility() != null ? command.visibility() : DEFAULT_VISIBILITY,
                command.maxParticipants(),
                blankToNull(command.coverKey()),
                command.currency(), // validada en el request
                EventStatus.ACTIVE, // siempre nace activo
                null, // createdAt: @CreationTimestamp
                null); // updatedAt: @UpdateTimestamp

        var saved = eventRepository.save(event);

        // Misma transacción que el INSERT del evento: o se guardan los dos o ninguno
        String payload = "{\"eventId\":\"%s\",\"ownerId\":\"%s\",\"categoryId\":%d,\"visibility\":\"%s\",\"startsAt\":\"%s\",\"lat\":%s,\"lon\":%s}"
                .formatted(saved.getId(), saved.getOwnerId(), saved.getCategoryId(),
                        saved.getVisibility().name().toLowerCase(Locale.ROOT), saved.getStartsAt(),
                        saved.getLat(), saved.getLon());

        outboxRepository.save(OutboxEvent.pending(OutboxEventType.EVENT_CREATED, payload));

        // Provisional: cuando tengáis R2 → fileStorage.getUrl(saved.getCoverKey())
        var coverUrl = saved.getCoverKey();

        return new CreateEventResult(saved, coverUrl);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
