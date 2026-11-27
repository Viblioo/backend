package com.revibe.backend.application.service.auth;

import static com.revibe.backend.application.service.auth.RateLimits.checkNotBlocked;

import com.revibe.backend.application.ports.input.auth.LoginCommand;
import com.revibe.backend.application.ports.input.auth.LoginResult;
import com.revibe.backend.application.ports.input.auth.LoginUseCase;
import com.revibe.backend.application.ports.output.security.PasswordHasherPort;
import com.revibe.backend.application.ports.output.security.RateLimit;
import com.revibe.backend.application.ports.output.security.RateLimiterPort;
import com.revibe.backend.application.ports.output.security.SessionPort;
import com.revibe.backend.application.ports.output.user.UserRepositoryPort;
import com.revibe.backend.domain.exception.AccountNotActiveException;
import com.revibe.backend.domain.exception.InvalidCredentialsException;
import com.revibe.backend.domain.model.PasswordPolicy;
import com.revibe.backend.domain.model.User;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginService implements LoginUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;
    private final SessionPort sessions;
    private final RateLimiterPort rateLimiter;

    /**
     * Hash con el que se compara cuando el usuario no existe. Así la respuesta tarda lo mismo (un BCrypt) exista o no,
     * y no se puede averiguar qué usernames están registrados midiendo tiempos.
     */
    private final String dummyHash;

    public LoginService(UserRepositoryPort userRepository, PasswordHasherPort passwordHasher, SessionPort sessions,
            RateLimiterPort rateLimiter) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.sessions = sessions;
        this.rateLimiter = rateLimiter;
        this.dummyHash = passwordHasher.hash(UUID.randomUUID().toString());
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResult login(LoginCommand command) {
        checkNotBlocked(rateLimiter, RateLimit.LOGIN_FAILURES_PER_IP, command.clientIp());

        // Mismo criterio que el registro: se guardan sin espacios y en minúsculas
        var login = command.login().strip().toLowerCase(Locale.ROOT);
        // Un username no puede llevar '@' (ver RegisterRequest), así que no hay ambigüedad
        Optional<User> user = login.contains("@")
                ? userRepository.findByEmail(login)
                : userRepository.findByUsername(login);

        // Por id si existe: entrar con username o con email cuenta para el mismo límite.
        // Si no existe, por lo que se ha escrito: también cuenta, si no el límite delataría qué cuentas existen.
        String accountKey = user.map(u -> u.getId().toString()).orElse(login);

        // Antes de mirar la contraseña: mientras dure el bloqueo ni la correcta entra, si no el límite no sirve
        checkNotBlocked(rateLimiter, RateLimit.LOGIN_FAILURES_PER_ACCOUNT, accountKey);

        if (!passwordMatches(user, command.rawPassword())) {
            rateLimiter.recordFailure(RateLimit.LOGIN_FAILURES_PER_ACCOUNT, accountKey);
            rateLimiter.recordFailure(RateLimit.LOGIN_FAILURES_PER_IP, command.clientIp());
            throw new InvalidCredentialsException();
        }

        // La contraseña es correcta: se olvidan los fallos de la cuenta aunque no esté activa
        rateLimiter.reset(RateLimit.LOGIN_FAILURES_PER_ACCOUNT, accountKey);

        // El estado se mira DESPUÉS de la contraseña: sin ella no se sabe ni si la cuenta existe
        checkCanLogIn(user.get());

        String token = sessions.create(user.get().getId());
        return new LoginResult(user.get(), token, sessions.ttl());
    }

    private boolean passwordMatches(Optional<User> user, String rawPassword) {
        if (PasswordPolicy.exceedsMaxBytes(rawPassword)) {
            return false; // no puede coincidir con ninguna contraseña guardada
        }
        if (user.isEmpty()) {
            passwordHasher.matches(rawPassword, dummyHash);
            return false;
        }
        return passwordHasher.matches(rawPassword, user.get().getPasswordHash());
    }

    private static void checkCanLogIn(User user) {
        switch (user.getStatus()) {
            case ACTIVE -> {
                // ok
            }
            case PENDING, SUSPENDED -> throw new AccountNotActiveException(user.getStatus());
            // Una cuenta borrada se comporta como si no existiera
            case DELETED -> throw new InvalidCredentialsException();
        }
    }
}
