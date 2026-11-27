package com.revibe.backend.infrastructure.adapters.output.persistence.postgres;

import com.revibe.backend.application.ports.output.event.EventRepositoryPort;
import com.revibe.backend.domain.exception.InvalidEventDataException;
import com.revibe.backend.domain.model.Event;
import com.revibe.backend.infrastructure.adapters.output.persistence.postgres.mapper.EventPersistenceMapper;
import com.revibe.backend.infrastructure.adapters.output.persistence.postgres.repository.EventJpaRepository;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventPersistenceAdapter implements EventRepositoryPort {

    // Nombre que PostgreSQL da a la FK de category_id si no se le pone uno (tabla_columna_fkey)
    private static final String FK_CATEGORY = "event_category_id_fkey";

    private final EventJpaRepository jpaRepository;
    private final EventPersistenceMapper mapper;

    @Override
    public Event save(Event event) {
        try {
            return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(event)));
        } catch (DataIntegrityViolationException ex) {
            throw translate(ex);
        }
    }

    @Override
    public Optional<Event> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<UUID> findIdsAfter(UUID afterId, int limit) {
        return jpaRepository.findIdsAfter(afterId, limit);
    }

    // transforma excepciones de la BD (500) en excepciones de negocio (400)
    private RuntimeException translate(DataIntegrityViolationException ex) {
        if (ex.getCause() instanceof ConstraintViolationException cve && cve.getConstraintName() != null) {
            String constraint = cve.getConstraintName().toLowerCase(Locale.ROOT);
            if (constraint.contains(FK_CATEGORY)) {
                return new InvalidEventDataException("categoryId", "La categoría no existe");
            }
        }
        return ex; // otra violación (CHECK, NOT NULL, owner inexistente...): es un bug nuestro, que salga como 500
    }
}
