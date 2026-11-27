package com.revibe.backend.application.service.auth;

import static com.revibe.backend.application.service.auth.RateLimits.acquire;

import com.revibe.backend.application.event.ActivationEmailRequestedEvent;
import com.revibe.backend.application.ports.input.auth.RegisterCommand;
import com.revibe.backend.application.ports.input.auth.RegisterResult;
import com.revibe.backend.application.ports.input.auth.RegisterUseCase;
import com.revibe.backend.application.ports.output.auth.ActivationCodePort;
import com.revibe.backend.application.ports.output.security.PasswordHasherPort;
import com.revibe.backend.application.ports.output.security.RateLimit;
import com.revibe.backend.application.ports.output.security.RateLimiterPort;
import com.revibe.backend.application.ports.output.user.UserRepositoryPort;
import com.revibe.backend.domain.exception.InvalidUserDataException;
import com.revibe.backend.domain.exception.UserAlreadyExistsException;
import com.revibe.backend.domain.model.PasswordPolicy;
import com.revibe.backend.domain.model.User;
import com.revibe.backend.domain.model.UserStatus;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterService implements RegisterUseCase {

    /** Edad mínima para registrarse (consentimiento digital en España: 14). */
    private static final int MIN_AGE = 14;

    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;
    private final ActivationCodePort activationCodes;
    private final RateLimiterPort rateLimiter;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Override
    @Transactional
    public RegisterResult register(RegisterCommand command) {
        acquire(rateLimiter, RateLimit.REGISTER_PER_IP, command.clientIp());

        if (Period.between(command.birthDate(), LocalDate.now(clock)).getYears() < MIN_AGE) {
            throw new InvalidUserDataException("birthDate", "Debes tener al menos " + MIN_AGE + " años");
        }

        PasswordPolicy.validate(command.rawPassword(), "password");

        var username = normalize(command.username());
        var email = normalize(command.email());

        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException(UserAlreadyExistsException.Field.USERNAME);
        }
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException(UserAlreadyExistsException.Field.EMAIL);
        }

        var user = new User(
                null,
                username,
                email,
                passwordHasher.hash(command.rawPassword()),
                command.firstName().strip(),
                blankToNull(command.lastName()),
                command.birthDate(),
                UserStatus.PENDING,
                null);

        var saved = userRepository.save(user);

        // El email sale tras el commit (AuthEmailListener). Si hay rollback, el código queda huérfano
        // en Redis y caduca solo.
        var activationCode = activationCodes.create(saved.getId());
        events.publishEvent(new ActivationEmailRequestedEvent(saved.getEmail(), activationCode));

        return new RegisterResult(saved);
    }

    private static String normalize(String value) {
        return value.strip().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
