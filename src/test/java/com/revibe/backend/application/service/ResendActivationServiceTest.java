package com.revibe.backend.application.service;

import static com.revibe.backend.application.ports.output.RateLimit.EMAIL_REQUESTS_PER_ADDRESS;
import static com.revibe.backend.application.ports.output.RateLimit.EMAIL_REQUESTS_PER_IP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.revibe.backend.application.event.ActivationEmailRequestedEvent;
import com.revibe.backend.application.ports.output.ActivationCodePort;
import com.revibe.backend.application.ports.output.RateLimiterPort;
import com.revibe.backend.application.ports.output.UserRepositoryPort;
import com.revibe.backend.domain.exception.TooManyRequestsException;
import com.revibe.backend.domain.model.UserStatus;
import com.revibe.backend.support.TestUsers;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ResendActivationServiceTest {

    private static final String IP = "10.0.0.1";

    @Mock
    private UserRepositoryPort users;
    @Mock
    private ActivationCodePort activationCodes;
    @Mock
    private RateLimiterPort limiter;
    @Mock
    private ApplicationEventPublisher events;

    private ResendActivationService service;

    @BeforeEach
    void setUp() {
        service = new ResendActivationService(users, activationCodes, limiter, events);
    }

    @Test
    void pendingUserGetsANewActivationEmail() {
        var user = TestUsers.user(UserStatus.PENDING);
        when(users.findByEmail("maria@example.com")).thenReturn(Optional.of(user));
        when(activationCodes.create(user.getId())).thenReturn("new-code");

        service.resend("MARIA@example.com", IP);

        var event = ArgumentCaptor.forClass(ActivationEmailRequestedEvent.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().email()).isEqualTo("maria@example.com");
        assertThat(event.getValue().activationCode()).isEqualTo("new-code");
    }

    @Test
    void alreadyActiveUserGetsNothing() {
        when(users.findByEmail("maria@example.com")).thenReturn(Optional.of(TestUsers.user(UserStatus.ACTIVE)));

        service.resend("maria@example.com", IP);

        verifyNoInteractions(activationCodes, events);
    }

    @Test
    void unknownEmailGetsNothingButStillCountsForTheLimits() {
        when(users.findByEmail("nadie@example.com")).thenReturn(Optional.empty());

        service.resend("nadie@example.com", IP);

        verify(limiter).tryAcquire(EMAIL_REQUESTS_PER_IP, IP);
        verify(limiter).tryAcquire(EMAIL_REQUESTS_PER_ADDRESS, "activation:nadie@example.com");
        verifyNoInteractions(activationCodes, events);
    }

    @Test
    void tooManyRequestsFromTheSameIpIsRejected() {
        when(limiter.tryAcquire(EMAIL_REQUESTS_PER_IP, IP)).thenReturn(Optional.of(Duration.ofMinutes(20)));

        assertThatThrownBy(() -> service.resend("maria@example.com", IP))
                .isInstanceOf(TooManyRequestsException.class);

        verifyNoInteractions(users, activationCodes, events);
    }
}
