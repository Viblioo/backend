package com.revibe.backend.infrastructure.adapters.output.persistence.postgres.mapper;

import com.revibe.backend.domain.model.Event;
import com.revibe.backend.infrastructure.adapters.output.persistence.postgres.entity.EventEntity;
import org.springframework.stereotype.Component;

@Component
public class EventPersistenceMapper {

    public EventEntity toEntity(Event event) {
        EventEntity entity = new EventEntity();
        entity.setId(event.getId());
        entity.setOwnerId(event.getOwnerId());
        entity.setTitle(event.getTitle());
        entity.setDescription(event.getDescription());
        entity.setCategoryId(event.getCategoryId());
        entity.setPlaceName(event.getPlaceName());
        entity.setLat(event.getLat());
        entity.setLon(event.getLon());
        entity.setStartsAt(event.getStartsAt());
        entity.setEndsAt(event.getEndsAt());
        entity.setVisibility(event.getVisibility()); // enum → enum, el converter hace el resto
        entity.setMaxParticipants(event.getMaxParticipants());
        entity.setCoverKey(event.getCoverKey());
        entity.setCurrency(event.getCurrency());
        entity.setStatus(event.getStatus()); // enum → enum
        entity.setCreatedAt(event.getCreatedAt());
        entity.setUpdatedAt(event.getUpdatedAt());
        return entity;
    }

    public Event toDomain(EventEntity entity) {
        return new Event(
                entity.getId(),
                entity.getOwnerId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getCategoryId(),
                entity.getPlaceName(),
                entity.getLat(),
                entity.getLon(),
                entity.getStartsAt(),
                entity.getEndsAt(),
                entity.getVisibility(),
                entity.getMaxParticipants(),
                entity.getCoverKey(),
                entity.getCurrency() != null ? entity.getCurrency().trim() : null,
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
