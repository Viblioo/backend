package com.revibe.backend.application.service;

import static com.revibe.backend.application.service.RateLimits.acquire;

import com.revibe.backend.application.event.PasswordResetRequestedEvent;
import com.revibe.backend.application.ports.input.PasswordResetUseCase;
import com.revibe.backend.application.ports.input.ResetPasswordCommand;
import com.revibe.backend.application.ports.output.PasswordHasherPort;
import com.revibe.backend.application.ports.output.PasswordResetCodePort;
import com.revibe.backend.application.ports.output.RateLimit;
import com.revibe.backend.application.ports.output.RateLimiterPort;
import com.revibe.backend.application.ports.output.SessionPort;
import com.revibe.backend.application.ports.output.UserRepositoryPort;
import com.revibe.backend.domain.exception.InvalidPasswordResetCodeException;
import com.revibe.backend.domain.model.PasswordPolicy;
import com.revibe.backend.domain.model.User;
import com.revibe.backend.domain.model.UserStatus;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recuperar la contraseña con un código de 6 cifras enviado por email. Es un código (y no un enlace) porque se teclea
 * en la app y no necesita una web donde aterrizar.
 */
@Service
@RequiredArgsConstructor
public class PasswordResetService implements PasswordResetUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordResetCodePort resetCodes;
    private final PasswordHasherPort passwordHasher;
    private final SessionPort sessions;
    private final RateLimiterPort rateLimiter;
    private final ApplicationEventPublisher events;

    @Override
    public void requestReset(String email, String clientIp) {
        var normalized = normalize(email);

        // Los límites se aplican exista o no el email: si no, el 429 delataría cuáles existen
        acquire(rateLimiter, RateLimit.EMAIL_REQUESTS_PER_IP, clientIp);
        acquire(rateLimiter, RateLimit.EMAIL_REQUESTS_PER_ADDRESS, "reset:" + normalized);

        // El email sale en otro hilo (AuthEmailListener): la respuesta tarda lo mismo exista o no
        findActive(normalized).ifPresent(user -> {
            var code = resetCodes.create(user.getId());
            events.publishEvent(new PasswordResetRequestedEvent(user.getEmail(), code));
        });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordCommand command) {
        // Antes que el código: una contraseña nueva mal puesta no debe gastar un intento
        PasswordPolicy.validate(command.newPassword(), "newPassword");

        var user = findActive(normalize(command.email())).orElseThrow(InvalidPasswordResetCodeException::new);
        if (command.code() == null || !resetCodes.verify(user.getId(), command.code().strip())) {
            throw new InvalidPasswordResetCodeException();
        }

        userRepository.updatePasswordHash(user.getId(), passwordHasher.hash(command.newPassword()));

        // Si alguien tenía la sesión robada, se queda fuera; y la cuenta deja de estar bloqueada por fallos
        sessions.revokeAll(user.getId());
        rateLimiter.reset(RateLimit.LOGIN_FAILURES_PER_ACCOUNT, user.getId().toString());
    }

    private Optional<User> findActive(String email) {
        return userRepository.findByEmail(email).filter(user -> user.getStatus() == UserStatus.ACTIVE);
    }

    private static String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
