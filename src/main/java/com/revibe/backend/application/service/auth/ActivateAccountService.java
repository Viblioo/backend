package com.revibe.backend.application.service.auth;

import com.revibe.backend.application.ports.input.auth.ActivateAccountUseCase;
import com.revibe.backend.application.ports.output.auth.ActivationCodePort;
import com.revibe.backend.application.ports.output.outbox.GraphOutboxPort;
import com.revibe.backend.application.ports.output.user.UserRepositoryPort;
import com.revibe.backend.domain.exception.InvalidActivationCodeException;
import com.revibe.backend.domain.model.OutboxEvent;
import com.revibe.backend.domain.model.OutboxEventType;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivateAccountService implements ActivateAccountUseCase {

    private final ActivationCodePort activationCodes;
    private final UserRepositoryPort userRepository;
    private final GraphOutboxPort outboxRepository;
    private final Clock clock;

    @Override
    @Transactional
    public void activate(String code) {
        if (code == null || code.isBlank()) {
            throw new InvalidActivationCodeException();
        }

        var userId = activationCodes.consume(code).orElseThrow(InvalidActivationCodeException::new);

        // Solo pasa de PENDING a ACTIVE: un usuario SUSPENDED o DELETED no se reactiva con un enlace viejo
        boolean activated = userRepository.activate(userId, Instant.now(clock));

        if (activated) {
            String payload = "{\"userId\":\"%s\"}".formatted(userId);
            outboxRepository.save(OutboxEvent.pending(OutboxEventType.USER_REGISTERED, payload));
        }

    }
}
