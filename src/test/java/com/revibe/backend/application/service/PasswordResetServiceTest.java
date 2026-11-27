package com.revibe.backend.application.service;

import static com.revibe.backend.application.ports.output.security.RateLimit.EMAIL_REQUESTS_PER_ADDRESS;
import static com.revibe.backend.application.ports.output.security.RateLimit.EMAIL_REQUESTS_PER_IP;
import static com.revibe.backend.application.ports.output.security.RateLimit.LOGIN_FAILURES_PER_ACCOUNT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.revibe.backend.application.event.PasswordResetRequestedEvent;
import com.revibe.backend.application.ports.input.auth.ResetPasswordCommand;
import com.revibe.backend.application.ports.output.security.PasswordHasherPort;
import com.revibe.backend.application.ports.output.security.PasswordResetCodePort;
import com.revibe.backend.application.ports.output.security.RateLimiterPort;
import com.revibe.backend.application.ports.output.security.SessionPort;
import com.revibe.backend.application.ports.output.user.UserRepositoryPort;
import com.revibe.backend.application.service.auth.PasswordResetService;
import com.revibe.backend.domain.exception.InvalidPasswordResetCodeException;
import com.revibe.backend.domain.exception.InvalidUserDataException;
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
class PasswordResetServiceTest {

    private static final String IP = "10.0.0.1";
    private static final String NEW_PASSWORD = "NuevaClave123";

    @Mock
    private UserRepositoryPort users;
    @Mock
    private PasswordResetCodePort resetCodes;
    @Mock
    private PasswordHasherPort hasher;
    @Mock
    private SessionPort sessions;
    @Mock
    private RateLimiterPort limiter;
    @Mock
    private ApplicationEventPublisher events;

    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(users, resetCodes, hasher, sessions, limiter, events);
    }

    // ---------- requestReset ----------

    @Test
    void activeUserGetsACodeByEmail() {
        var user = TestUsers.user(UserStatus.ACTIVE);
        when(users.findByEmail("maria@example.com")).thenReturn(Optional.of(user));
        when(resetCodes.create(user.getId())).thenReturn("123456");

        service.requestReset(" Maria@Example.com ", IP);

        var event = ArgumentCaptor.forClass(PasswordResetRequestedEvent.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().email()).isEqualTo("maria@example.com");
        assertThat(event.getValue().code()).isEqualTo("123456");
    }

    @Test
    void unknownEmailDoesNothingButStillCountsForTheLimits() {
        when(users.findByEmail("nadie@example.com")).thenReturn(Optional.empty());

        service.requestReset("nadie@example.com", IP);

        verify(limiter).tryAcquire(EMAIL_REQUESTS_PER_IP, IP);
        verify(limiter).tryAcquire(EMAIL_REQUESTS_PER_ADDRESS, "reset:nadie@example.com");
        verifyNoInteractions(resetCodes, events);
    }

    @Test
    void pendingUserDoesNotGetACode() {
        when(users.findByEmail("maria@example.com")).thenReturn(Optional.of(TestUsers.user(UserStatus.PENDING)));

        service.requestReset("maria@example.com", IP);

        verifyNoInteractions(resetCodes, events);
    }

    @Test
    void tooManyRequestsForTheSameEmailIsRejected() {
        when(limiter.tryAcquire(EMAIL_REQUESTS_PER_IP, IP)).thenReturn(Optional.empty());
        when(limiter.tryAcquire(EMAIL_REQUESTS_PER_ADDRESS, "reset:maria@example.com"))
                .thenReturn(Optional.of(Duration.ofMinutes(30)));

        assertThatThrownBy(() -> service.requestReset("maria@example.com", IP))
                .isInstanceOf(TooManyRequestsException.class);

        verifyNoInteractions(users, resetCodes, events);
    }

    // ---------- resetPassword ----------

    @Test
    void correctCodeChangesPasswordClosesAllSessionsAndUnblocksLogin() {
        var user = TestUsers.user(UserStatus.ACTIVE);
        when(users.findByEmail("maria@example.com")).thenReturn(Optional.of(user));
        when(resetCodes.verify(user.getId(), "123456")).thenReturn(true);
        when(hasher.hash(NEW_PASSWORD)).thenReturn("new-hash");

        service.resetPassword(new ResetPasswordCommand("maria@example.com", " 123456 ", NEW_PASSWORD));

        verify(users).updatePasswordHash(user.getId(), "new-hash");
        verify(sessions).revokeAll(user.getId());
        verify(limiter).reset(LOGIN_FAILURES_PER_ACCOUNT, user.getId().toString());
    }

    @Test
    void wrongCodeDoesNotChangeThePassword() {
        var user = TestUsers.user(UserStatus.ACTIVE);
        when(users.findByEmail("maria@example.com")).thenReturn(Optional.of(user));
        when(resetCodes.verify(user.getId(), "000000")).thenReturn(false);

        assertThatThrownBy(() -> service
                .resetPassword(new ResetPasswordCommand("maria@example.com", "000000", NEW_PASSWORD)))
                .isInstanceOf(InvalidPasswordResetCodeException.class);

        verify(users, never()).updatePasswordHash(any(), any());
        verifyNoInteractions(sessions);
    }

    @Test
    void unknownEmailGivesTheSameErrorAsAWrongCode() {
        when(users.findByEmail("nadie@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service
                .resetPassword(new ResetPasswordCommand("nadie@example.com", "123456", NEW_PASSWORD)))
                .isInstanceOf(InvalidPasswordResetCodeException.class);

        verifyNoInteractions(resetCodes);
    }

    @Test
    void invalidNewPasswordIsRejectedWithoutSpendingACodeAttempt() {
        assertThatThrownBy(() -> service
                .resetPassword(new ResetPasswordCommand("maria@example.com", "123456", "corta")))
                .isInstanceOf(InvalidUserDataException.class)
                .extracting(e -> ((InvalidUserDataException) e).getField())
                .isEqualTo("newPassword");

        verifyNoInteractions(resetCodes, users);
    }
}
