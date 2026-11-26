package com.revibe.backend.infrastructure.adapters.output.persistence.postgres.converter;

import com.revibe.backend.domain.model.UserStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Locale;

/**
 * UserStatus.PENDING en Java <-> 'pending' en la columna users.status (ver CHECK de V1).
 */
@Converter
public class UserStatusConverter implements AttributeConverter<UserStatus, String> {

    @Override
    public String convertToDatabaseColumn(UserStatus status) {
        return status == null ? null : status.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public UserStatus convertToEntityAttribute(String value) {
        return value == null ? null : UserStatus.valueOf(value.toUpperCase(Locale.ROOT));
    }
}
