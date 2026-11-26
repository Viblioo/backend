package com.revibe.backend.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class OutboxEventTest {

    @Test
    void newEventIsPendingWithoutIdNorAttempts() {
        var e = OutboxEvent.pending(OutboxEventType.USER_REGISTERED, "{}");

        assertThat(e.getId()).isNull();
        assertThat(e.isPending()).isTrue();
        assertThat(e.getAttempts()).isZero();
    }

    @Test
    void failingKeepsItPendingAndCountsAttempts() {
        var e = OutboxEvent.pending(OutboxEventType.USER_REGISTERED, "{}");

        e.markFailed("uno");
        e.markFailed("dos");

        assertThat(e.isPending()).isTrue();
        assertThat(e.getAttempts()).isEqualTo(2);
        assertThat(e.getLastError()).isEqualTo("dos");
    }

    @Test
    void processingClearsTheLastError() {
        var e = OutboxEvent.pending(OutboxEventType.USER_REGISTERED, "{}");
        e.markFailed("error");

        e.markProcessed(Instant.parse("2026-11-26T12:00:00Z"));

        assertThat(e.isPending()).isFalse();
        assertThat(e.getLastError()).isNull();
    }
}
