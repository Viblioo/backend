package com.revibe.backend.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.revibe.backend.domain.exception.InvalidUserDataException;
import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    @Test
    void acceptsAValidPassword() {
        assertThatCode(() -> PasswordPolicy.validate("Secreta123!", "password")).doesNotThrowAnyException();
    }

    @Test
    void rejectsShortPasswordsOnTheGivenField() {
        assertThatThrownBy(() -> PasswordPolicy.validate("1234567", "newPassword"))
                .isInstanceOf(InvalidUserDataException.class)
                .extracting(e -> ((InvalidUserDataException) e).getField())
                .isEqualTo("newPassword");
    }

    @Test
    void the72LimitIsInBytesNotCharacters() {
        String exactly72Bytes = "ñ".repeat(36); // 36 caracteres, 72 bytes
        String over72Bytes = "ñ".repeat(37); // 37 caracteres, 74 bytes

        assertThat(PasswordPolicy.exceedsMaxBytes(exactly72Bytes)).isFalse();
        assertThat(PasswordPolicy.exceedsMaxBytes(over72Bytes)).isTrue();
        assertThatThrownBy(() -> PasswordPolicy.validate(over72Bytes, "password"))
                .isInstanceOf(InvalidUserDataException.class);
    }
}
