package com.revibe.backend.infrastructure.adapters.output.persistence.postgres.mapper;

import com.revibe.backend.domain.model.OutboxEvent;
import com.revibe.backend.infrastructure.adapters.output.persistence.postgres.entity.OutboxEntity;
import org.springframework.stereotype.Component;

/**
 * Traduce entre el OutboxEvent del dominio y la entidad JPA de graph_outbox.
 */
@Component
public class OutboxPersistenceMapper {

    public OutboxEntity toEntity(OutboxEvent event) {
        OutboxEntity entity = new OutboxEntity();
        entity.setId(event.getId());
        entity.setEventType(event.getEventType());
        entity.setPayload(event.getPayload());
        entity.setCreatedAt(event.getCreatedAt());
        entity.setProcessedAt(event.getProcessedAt());
        entity.setAttempts(event.getAttempts());
        entity.setLastError(event.getLastError());
        return entity;
    }

    public OutboxEvent toDomain(OutboxEntity entity) {
        return new OutboxEvent(entity.getId(), entity.getEventType(), entity.getPayload(), entity.getCreatedAt(),
                entity.getProcessedAt(), entity.getAttempts(), entity.getLastError());
    }
}
