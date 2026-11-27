package com.revibe.backend.application.service;

import static com.revibe.backend.application.ports.output.security.RateLimit.REGISTER_PER_IP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.revibe.backend.application.event.ActivationEmailRequestedEvent;
import com.revibe.backend.application.ports.input.auth.RegisterCommand;
import com.revibe.backend.application.ports.output.auth.ActivationCodePort;
import com.revibe.backend.application.ports.output.security.PasswordHasherPort;
import com.revibe.backend.application.ports.output.security.RateLimiterPort;
import com.revibe.backend.application.ports.output.user.UserRepositoryPort;
import com.revibe.backend.application.service.auth.RegisterService;
import com.revibe.backend.domain.exception.InvalidUserDataException;
import com.revibe.backend.domain.exception.TooManyRequestsException;
import com.revibe.backend.domain.exception.UserAlreadyExistsException;
import com.revibe.backend.domain.model.User;
import com.revibe.backend.domain.model.UserStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class RegisterServiceTest {

    private static final String IP = "10.0.0.1";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-26T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private UserRepositoryPort users;
    @Mock
    private PasswordHasherPort hasher;
    @Mock
    private ActivationCodePort activationCodes;
    @Mock
    private RateLimiterPort limiter;
    @Mock
    private ApplicationEventPublisher events;

    private RegisterService service;

    @BeforeEach
    void setUp() {
        service = new RegisterService(users, hasher, activationCodes, limiter, events, CLOCK);
    }

    private static RegisterCommand command(LocalDate birthDate) {
        return new RegisterCommand(" Maria_Lopez ", " Maria@Example.com ", "Secreta123!", " María ", "  ",
                birthDate, IP);
    }

    @Test
    void createsPendingUserNormalizedAndRequestsActivationEmail() {
        when(hasher.hash("Secreta123!")).thenReturn("hash");
        when(users.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return new User(UUID.randomUUID(), u.getUsername(), u.getEmail(), u.getPasswordHash(),
                    u.getFirstName(), u.getLastName(), u.getBirthDate(), u.getStatus(), Instant.now(CLOCK));
        });
        when(activationCodes.create(any())).thenReturn("activation-code");

        var result = service.register(command(LocalDate.of(2000, 5, 14)));

        var saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().getUsername()).isEqualTo("maria_lopez");
        assertThat(saved.getValue().getEmail()).isEqualTo("maria@example.com");
        assertThat(saved.getValue().getFirstName()).isEqualTo("María");
        assertThat(saved.getValue().getLastName()).isNull();
        assertThat(saved.getValue().getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hash");

        var event = ArgumentCaptor.forClass(ActivationEmailRequestedEvent.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().email()).isEqualTo("maria@example.com");
        assertThat(event.getValue().activationCode()).isEqualTo("activation-code");
        assertThat(result.user().getId()).isNotNull();
    }

    @Test
    void under14IsRejected() {
        // 26/11/2026: nacida el 27/11/2012 tiene 13 años
        assertThatThrownBy(() -> service.register(command(LocalDate.of(2012, 11, 27))))
                .isInstanceOf(InvalidUserDataException.class)
                .extracting(e -> ((InvalidUserDataException) e).getField())
                .isEqualTo("birthDate");
        verify(users, never()).save(any());
    }

    @Test
    void exactly14IsAccepted() {
        when(hasher.hash(any())).thenReturn("hash");
        when(users.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(activationCodes.create(any())).thenReturn("c");

        // El save devuelve el mismo usuario sin id: el código se pide con id null, da igual para este test
        service.register(command(LocalDate.of(2012, 11, 26)));

        verify(users).save(any());
    }

    @Test
    void duplicateUsernameIsRejected() {
        when(users.existsByUsername("maria_lopez")).thenReturn(true);

        assertThatThrownBy(() -> service.register(command(LocalDate.of(2000, 1, 1))))
                .isInstanceOf(UserAlreadyExistsException.class)
                .extracting(e -> ((UserAlreadyExistsException) e).getField())
                .isEqualTo(UserAlreadyExistsException.Field.USERNAME);
        verify(users, never()).save(any());
    }

    @Test
    void tooManyRegistrationsFromTheSameIpAreRejected() {
        when(limiter.tryAcquire(REGISTER_PER_IP, IP)).thenReturn(Optional.of(Duration.ofMinutes(40)));

        assertThatThrownBy(() -> service.register(command(LocalDate.of(2000, 1, 1))))
                .isInstanceOf(TooManyRequestsException.class);
        verifyNoInteractions(users, events);
    }
}
