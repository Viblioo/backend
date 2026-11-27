package com.revibe.backend.infrastructure.adapters.output.persistence.postgres.converter;

import com.revibe.backend.domain.model.EventVisibility;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Locale;

/**
 * EventVisibility.PUBLIC en Java <-> 'public' en la columna event.visibility.
 */
@Converter
public class EventVisibilityConverter implements AttributeConverter<EventVisibility, String> {

    @Override
    public String convertToDatabaseColumn(EventVisibility visibility) {
        return visibility == null ? null : visibility.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public EventVisibility convertToEntityAttribute(String value) {
        return value == null ? null : EventVisibility.valueOf(value.toUpperCase(Locale.ROOT));
    }
}
