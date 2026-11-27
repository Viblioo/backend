package com.revibe.backend.application.ports.output.event;

import com.revibe.backend.domain.model.Event;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepositoryPort {

    /**
     * Guarda el evento y devuelve el resultado con id, createdAt y updatedAt. Si la BD rechaza el insert (p. ej.
     * categoryId que no existe) lanza DataIntegrityViolationException.
     */
    Event save(Event event);

    Optional<Event> findById(UUID id);

    /**
     * Ids de eventos con id mayor que {@code afterId}, ordenados por id (paginación por clave). Para recorrer todos,
     * empezar con el UUID 00000000-... y pasar el último id de cada página.
     */
    List<UUID> findIdsAfter(UUID afterId, int limit);
}
