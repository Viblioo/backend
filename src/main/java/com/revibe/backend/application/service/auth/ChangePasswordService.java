package com.revibe.backend.application.service.auth;

import static com.revibe.backend.application.service.auth.RateLimits.checkNotBlocked;

import com.revibe.backend.application.ports.input.auth.ChangePasswordCommand;
import com.revibe.backend.application.ports.input.auth.ChangePasswordUseCase;
import com.revibe.backend.application.ports.output.security.PasswordHasherPort;
import com.revibe.backend.application.ports.output.security.RateLimit;
import com.revibe.backend.application.ports.output.security.RateLimiterPort;
import com.revibe.backend.application.ports.output.security.SessionPort;
import com.revibe.backend.application.ports.output.user.UserRepositoryPort;
import com.revibe.backend.domain.exception.InvalidUserDataException;
import com.revibe.backend.domain.exception.UserNotFoundException;
import com.revibe.backend.domain.model.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChangePasswordService implements ChangePasswordUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;
    private final SessionPort sessions;
    private final RateLimiterPort rateLimiter;

    @Override
    @Transactional
    public void changePassword(ChangePasswordCommand command) {
        String limitKey = command.userId().toString();
        // Con una sesión robada no se debe poder probar contraseñas sin límite
        checkNotBlocked(rateLimiter, RateLimit.CHANGE_PASSWORD_FAILURES_PER_USER, limitKey);

        var user = userRepository.findById(command.userId()).orElseThrow(UserNotFoundException::new);

        if (PasswordPolicy.exceedsMaxBytes(command.currentPassword())
                || !passwordHasher.matches(command.currentPassword(), user.getPasswordHash())) {
            rateLimiter.recordFailure(RateLimit.CHANGE_PASSWORD_FAILURES_PER_USER, limitKey);
            // 400 y no 401: un 401 haría creer a la app que la sesión ha caducado
            throw new InvalidUserDataException("currentPassword", "La contraseña actual no es correcta");
        }
        rateLimiter.reset(RateLimit.CHANGE_PASSWORD_FAILURES_PER_USER, limitKey);

        PasswordPolicy.validate(command.newPassword(), "newPassword");

        userRepository.updatePasswordHash(user.getId(), passwordHasher.hash(command.newPassword()));
        // Los demás dispositivos tienen que volver a entrar; este sigue con su sesión
        sessions.revokeOthers(user.getId(), command.currentToken());
    }
}
