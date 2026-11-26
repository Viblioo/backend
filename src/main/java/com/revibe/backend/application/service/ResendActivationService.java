package com.revibe.backend.application.service;

import static com.revibe.backend.application.service.RateLimits.acquire;

import com.revibe.backend.application.event.ActivationEmailRequestedEvent;
import com.revibe.backend.application.ports.input.ResendActivationUseCase;
import com.revibe.backend.application.ports.output.ActivationCodePort;
import com.revibe.backend.application.ports.output.RateLimit;
import com.revibe.backend.application.ports.output.RateLimiterPort;
import com.revibe.backend.application.ports.output.UserRepositoryPort;
import com.revibe.backend.domain.model.UserStatus;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResendActivationService implements ResendActivationUseCase {

    private final UserRepositoryPort userRepository;
    private final ActivationCodePort activationCodes;
    private final RateLimiterPort rateLimiter;
    private final ApplicationEventPublisher events;

    @Override
    public void resend(String email, String clientIp) {
        var normalized = email.strip().toLowerCase(Locale.ROOT);

        // Los límites se aplican exista o no el email: si no, el 429 delataría cuáles existen
        acquire(rateLimiter, RateLimit.EMAIL_REQUESTS_PER_IP, clientIp);
        acquire(rateLimiter, RateLimit.EMAIL_REQUESTS_PER_ADDRESS, "activation:" + normalized);

        userRepository.findByEmail(normalized)
                .filter(user -> user.getStatus() == UserStatus.PENDING)
                .ifPresent(user -> {
                    // Los códigos anteriores siguen valiendo hasta que caduquen: cualquiera de ellos activa la cuenta
                    var code = activationCodes.create(user.getId());
                    events.publishEvent(new ActivationEmailRequestedEvent(user.getEmail(), code));
                });
    }
}
