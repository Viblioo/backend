package com.revibe.backend.application.service;

import static com.revibe.backend.application.ports.output.RateLimit.CHANGE_PASSWORD_FAILURES_PER_USER;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.revibe.backend.application.ports.input.ChangePasswordCommand;
import com.revibe.backend.application.ports.output.PasswordHasherPort;
import com.revibe.backend.application.ports.output.RateLimiterPort;
import com.revibe.backend.application.ports.output.SessionPort;
import com.revibe.backend.application.ports.output.UserRepositoryPort;
import com.revibe.backend.domain.exception.InvalidUserDataException;
import com.revibe.backend.domain.exception.TooManyRequestsException;
import com.revibe.backend.domain.model.User;
import com.revibe.backend.domain.model.UserStatus;
import com.revibe.backend.support.TestUsers;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChangePasswordServiceTest {

    @Mock
    private UserRepositoryPort users;
    @Mock
    private PasswordHasherPort hasher;
    @Mock
    private SessionPort sessions;
    @Mock
    private RateLimiterPort limiter;

    private ChangePasswordService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new ChangePasswordService(users, hasher, sessions, limiter);
        user = TestUsers.user(UserStatus.ACTIVE);
    }

    @Test
    void correctCurrentPasswordChangesItAndClosesOtherSessions() {
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(hasher.matches("Actual123", TestUsers.PASSWORD_HASH)).thenReturn(true);
        when(hasher.hash("NuevaClave123")).thenReturn("new-hash");

        service.changePassword(new ChangePasswordCommand(user.getId(), "my-token", "Actual123", "NuevaClave123"));

        verify(users).updatePasswordHash(user.getId(), "new-hash");
        verify(sessions).revokeOthers(user.getId(), "my-token");
        verify(sessions, never()).revokeAll(any());
        verify(limiter).reset(CHANGE_PASSWORD_FAILURES_PER_USER, user.getId().toString());
    }

    @Test
    void wrongCurrentPasswordIsA400OnCurrentPasswordAndIsCounted() {
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(hasher.matches("mala", TestUsers.PASSWORD_HASH)).thenReturn(false);

        assertThatThrownBy(() -> service
                .changePassword(new ChangePasswordCommand(user.getId(), "t", "mala", "NuevaClave123")))
                .isInstanceOf(InvalidUserDataException.class)
                .extracting(e -> ((InvalidUserDataException) e).getField())
                .isEqualTo("currentPassword");

        verify(limiter).recordFailure(CHANGE_PASSWORD_FAILURES_PER_USER, user.getId().toString());
        verify(users, never()).updatePasswordHash(any(), any());
        verifyNoInteractions(sessions);
    }

    @Test
    void blockedUserCannotEvenTry() {
        when(limiter.blockedFor(CHANGE_PASSWORD_FAILURES_PER_USER, user.getId().toString()))
                .thenReturn(Optional.of(Duration.ofMinutes(15)));

        assertThatThrownBy(() -> service
                .changePassword(new ChangePasswordCommand(user.getId(), "t", "Actual123", "NuevaClave123")))
                .isInstanceOf(TooManyRequestsException.class);

        verifyNoInteractions(users, hasher, sessions);
    }

    @Test
    void invalidNewPasswordIsRejected() {
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(hasher.matches("Actual123", TestUsers.PASSWORD_HASH)).thenReturn(true);

        assertThatThrownBy(() -> service
                .changePassword(new ChangePasswordCommand(user.getId(), "t", "Actual123", "corta")))
                .isInstanceOf(InvalidUserDataException.class)
                .extracting(e -> ((InvalidUserDataException) e).getField())
                .isEqualTo("newPassword");

        verify(users, never()).updatePasswordHash(any(), any());
    }
}
