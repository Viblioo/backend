package com.revibe.backend.application.ports.input.event;

import com.revibe.backend.domain.model.Event;

/** Resultado de crear un evento: el evento creado y la URL de su portada (si tiene). */
public record CreateEventResult(Event event, String coverUrl) {

    // Evita que la URL firmada acabe en los logs si alguien hace log.info(result)
    @Override
    public String toString() {
        return "CreateEventResult[eventId=%s]".formatted(event.getId());
    }
}
