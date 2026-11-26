package com.revibe.backend.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.revibe.backend.application.ports.output.ActivationCodePort;
import com.revibe.backend.application.ports.output.GraphOutboxPort;
import com.revibe.backend.application.ports.output.UserRepositoryPort;
import com.revibe.backend.domain.exception.InvalidActivationCodeException;
import com.revibe.backend.domain.model.OutboxEvent;
import com.revibe.backend.domain.model.OutboxEventType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActivateAccountServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-26T12:00:00Z");

    @Mock
    private ActivationCodePort activationCodes;
    @Mock
    private UserRepositoryPort users;
    @Mock
    private GraphOutboxPort outbox;

    private ActivateAccountService service;

    @BeforeEach
    void setUp() {
        service = new ActivateAccountService(activationCodes, users, outbox, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void validCodeActivatesUserAndQueuesTheGraphNode() {
        var userId = UUID.randomUUID();
        when(activationCodes.consume("code")).thenReturn(Optional.of(userId));
        when(users.activate(userId, NOW)).thenReturn(true);

        service.activate("code");

        var event = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outbox).save(event.capture());
        assertThat(event.getValue().getEventType()).isEqualTo(OutboxEventType.USER_REGISTERED);
        assertThat(event.getValue().getPayload()).isEqualTo("{\"userId\":\"" + userId + "\"}");
        assertThat(event.getValue().getId()).isNull();
    }

    @Test
    void userThatWasNotPendingIsNotQueuedForTheGraph() {
        var userId = UUID.randomUUID();
        when(activationCodes.consume("code")).thenReturn(Optional.of(userId));
        when(users.activate(userId, NOW)).thenReturn(false);

        service.activate("code");

        verify(outbox, never()).save(any());
    }

    @Test
    void unknownOrExpiredCodeIsRejected() {
        when(activationCodes.consume("old")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activate("old")).isInstanceOf(InvalidActivationCodeException.class);

        verifyNoInteractions(users, outbox);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void missingCodeIsRejectedWithoutTouchingRedis(String code) {
        assertThatThrownBy(() -> service.activate(code)).isInstanceOf(InvalidActivationCodeException.class);

        verifyNoInteractions(activationCodes, users, outbox);
    }
}
