package com.revibe.backend.infrastructure.adapters.output.persistence.postgres.converter;

import com.revibe.backend.domain.model.EventStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Locale;

/**
 * EventStatus.ACTIVE en Java <-> 'active' en la columna event.status.
 */
@Converter
public class EventStatusConverter implements AttributeConverter<EventStatus, String> {

    @Override
    public String convertToDatabaseColumn(EventStatus status) {
        return status == null ? null : status.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public EventStatus convertToEntityAttribute(String value) {
        return value == null ? null : EventStatus.valueOf(value.toUpperCase(Locale.ROOT));
    }
}
