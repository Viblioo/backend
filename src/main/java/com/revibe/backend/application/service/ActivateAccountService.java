package com.revibe.backend.application.service;

import com.revibe.backend.application.ports.output.ActivationCodePort;
import com.revibe.backend.application.ports.output.UserRepositoryPort;
import com.revibe.backend.application.usecases.ActivateAccountUseCase;
import com.revibe.backend.domain.exception.InvalidActivationCodeException;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivateAccountService implements ActivateAccountUseCase {

    private final ActivationCodePort activationCodes;
    private final UserRepositoryPort userRepository;
    private final Clock clock;

    @Override
    @Transactional
    public void activate(String code) {
        if (code == null || code.isBlank()) {
            throw new InvalidActivationCodeException();
        }

        var userId = activationCodes.consume(code).orElseThrow(InvalidActivationCodeException::new);

        // Solo pasa de PENDING a ACTIVE: un usuario SUSPENDED o DELETED no se reactiva con un enlace viejo
        userRepository.activate(userId, Instant.now(clock));
    }
}
