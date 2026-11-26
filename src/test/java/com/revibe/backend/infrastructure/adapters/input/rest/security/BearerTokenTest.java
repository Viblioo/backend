package com.revibe.backend.infrastructure.adapters.input.rest.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class BearerTokenTest {

    @Test
    void extractsTheToken() {
        assertThat(BearerToken.from("Bearer abc123")).contains("abc123");
    }

    @Test
    void trimsSpacesAroundTheToken() {
        assertThat(BearerToken.from("Bearer   abc123  ")).contains("abc123");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Bearer", "Bearer ", "Bearer    ", "Basic abc123", "bearer abc123", "abc123"})
    void anythingElseIsEmpty(String header) {
        assertThat(BearerToken.from(header)).isEmpty();
    }
}
