package com.revibe.backend.application.service;

import static com.revibe.backend.application.ports.output.RateLimit.LOGIN_FAILURES_PER_ACCOUNT;
import static com.revibe.backend.application.ports.output.RateLimit.LOGIN_FAILURES_PER_IP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.revibe.backend.application.ports.input.LoginCommand;
import com.revibe.backend.application.ports.output.PasswordHasherPort;
import com.revibe.backend.application.ports.output.RateLimiterPort;
import com.revibe.backend.application.ports.output.SessionPort;
import com.revibe.backend.application.ports.output.UserRepositoryPort;
import com.revibe.backend.domain.exception.AccountNotActiveException;
import com.revibe.backend.domain.exception.InvalidCredentialsException;
import com.revibe.backend.domain.exception.TooManyRequestsException;
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
class LoginServiceTest {

    private static final String IP = "10.0.0.1";
    private static final String PASSWORD = "Secreta123!";
    private static final String DUMMY_HASH = "dummy-hash";

    @Mock
    private UserRepositoryPort users;
    @Mock
    private PasswordHasherPort hasher;
    @Mock
    private SessionPort sessions;
    @Mock
    private RateLimiterPort limiter;

    private LoginService service;

    @BeforeEach
    void setUp() {
        when(hasher.hash(anyString())).thenReturn(DUMMY_HASH); // el hash señuelo del constructor
        service = new LoginService(users, hasher, sessions, limiter);
    }

    @Test
    void activeUserWithCorrectPasswordGetsASessionAndCounterIsReset() {
        var user = TestUsers.user(UserStatus.ACTIVE);
        when(users.findByUsername("maria")).thenReturn(Optional.of(user));
        when(hasher.matches(PASSWORD, TestUsers.PASSWORD_HASH)).thenReturn(true);
        when(sessions.create(user.getId())).thenReturn("token-123");
        when(sessions.ttl()).thenReturn(Duration.ofDays(30));

        var result = service.login(new LoginCommand("  Maria ", PASSWORD, IP));

        assertThat(result.token()).isEqualTo("token-123");
        assertThat(result.user()).isSameAs(user);
        assertThat(result.expiresIn()).isEqualTo(Duration.ofDays(30));
        verify(limiter).reset(LOGIN_FAILURES_PER_ACCOUNT, user.getId().toString());
        verify(limiter, never()).recordFailure(any(), any());
    }

    @Test
    void loginWithEmailLooksUpByEmail() {
        var user = TestUsers.user(UserStatus.ACTIVE);
        when(users.findByEmail("maria@example.com")).thenReturn(Optional.of(user));
        when(hasher.matches(PASSWORD, TestUsers.PASSWORD_HASH)).thenReturn(true);
        when(sessions.create(user.getId())).thenReturn("token");
        when(sessions.ttl()).thenReturn(Duration.ofDays(30));

        var result = service.login(new LoginCommand("Maria@Example.com", PASSWORD, IP));

        assertThat(result.user()).isSameAs(user);
        verify(users, never()).findByUsername(any());
    }

    @Test
    void wrongPasswordIsRejectedAndCountedForAccountAndIp() {
        var user = TestUsers.user(UserStatus.ACTIVE);
        when(users.findByUsername("maria")).thenReturn(Optional.of(user));
        when(hasher.matches("mala", TestUsers.PASSWORD_HASH)).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginCommand("maria", "mala", IP)))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(limiter).recordFailure(LOGIN_FAILURES_PER_ACCOUNT, user.getId().toString());
        verify(limiter).recordFailure(LOGIN_FAILURES_PER_IP, IP);
        verify(sessions, never()).create(any());
    }

    @Test
    void unknownUserComparesAgainstDummyHashAndIsCountedByWhatWasTyped() {
        when(users.findByUsername("nadie")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginCommand("nadie", PASSWORD, IP)))
                .isInstanceOf(InvalidCredentialsException.class);

        // Mismo trabajo (un BCrypt) que si existiera: no se distingue por tiempo
        verify(hasher).matches(PASSWORD, DUMMY_HASH);
        verify(limiter).recordFailure(LOGIN_FAILURES_PER_ACCOUNT, "nadie");
        verify(limiter).recordFailure(LOGIN_FAILURES_PER_IP, IP);
    }

    @Test
    void blockedAccountIsRejectedEvenWithCorrectPasswordAndPasswordIsNotChecked() {
        var user = TestUsers.user(UserStatus.ACTIVE);
        when(users.findByUsername("maria")).thenReturn(Optional.of(user));
        when(limiter.blockedFor(LOGIN_FAILURES_PER_IP, IP)).thenReturn(Optional.empty());
        when(limiter.blockedFor(LOGIN_FAILURES_PER_ACCOUNT, user.getId().toString()))
                .thenReturn(Optional.of(Duration.ofMinutes(10)));

        assertThatThrownBy(() -> service.login(new LoginCommand("maria", PASSWORD, IP)))
                .isInstanceOf(TooManyRequestsException.class)
                .extracting(e -> ((TooManyRequestsException) e).getRetryAfter())
                .isEqualTo(Duration.ofMinutes(10));

        verify(hasher, never()).matches(any(), any());
        verify(sessions, never()).create(any());
    }

    @Test
    void blockedIpIsRejectedBeforeLookingUpTheUser() {
        when(limiter.blockedFor(LOGIN_FAILURES_PER_IP, IP)).thenReturn(Optional.of(Duration.ofMinutes(5)));

        assertThatThrownBy(() -> service.login(new LoginCommand("maria", PASSWORD, IP)))
                .isInstanceOf(TooManyRequestsException.class);

        verifyNoInteractions(users, sessions);
    }

    @Test
    void pendingUserWithCorrectPasswordGets403AndCounterIsReset() {
        var user = TestUsers.user(UserStatus.PENDING);
        when(users.findByUsername("maria")).thenReturn(Optional.of(user));
        when(hasher.matches(PASSWORD, TestUsers.PASSWORD_HASH)).thenReturn(true);

        assertThatThrownBy(() -> service.login(new LoginCommand("maria", PASSWORD, IP)))
                .isInstanceOf(AccountNotActiveException.class)
                .extracting(e -> ((AccountNotActiveException) e).getStatus())
                .isEqualTo(UserStatus.PENDING);

        verify(limiter).reset(LOGIN_FAILURES_PER_ACCOUNT, user.getId().toString());
        verify(sessions, never()).create(any());
    }

    @Test
    void suspendedUserWithCorrectPasswordGets403() {
        var user = TestUsers.user(UserStatus.SUSPENDED);
        when(users.findByUsername("maria")).thenReturn(Optional.of(user));
        when(hasher.matches(PASSWORD, TestUsers.PASSWORD_HASH)).thenReturn(true);

        assertThatThrownBy(() -> service.login(new LoginCommand("maria", PASSWORD, IP)))
                .isInstanceOf(AccountNotActiveException.class);
    }

    @Test
    void deletedUserBehavesAsIfItDidNotExist() {
        var user = TestUsers.user(UserStatus.DELETED);
        when(users.findByUsername("maria")).thenReturn(Optional.of(user));
        when(hasher.matches(PASSWORD, TestUsers.PASSWORD_HASH)).thenReturn(true);

        assertThatThrownBy(() -> service.login(new LoginCommand("maria", PASSWORD, IP)))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(sessions, never()).create(any());
    }

    @Test
    void passwordLongerThan72BytesNeverMatchesAndCountsAsFailure() {
        var user = TestUsers.user(UserStatus.ACTIVE);
        when(users.findByUsername("maria")).thenReturn(Optional.of(user));
        String tooLong = "ñ".repeat(37); // 74 bytes en UTF-8

        assertThatThrownBy(() -> service.login(new LoginCommand("maria", tooLong, IP)))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(hasher, never()).matches(any(), any());
        verify(limiter).recordFailure(LOGIN_FAILURES_PER_ACCOUNT, user.getId().toString());
    }
}
