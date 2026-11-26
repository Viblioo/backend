package com.revibe.backend.infrastructure.adapters.output.email;

import com.revibe.backend.application.ports.output.EmailSenderPort;
import com.revibe.backend.application.usecases.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Envía el email de activación DESPUÉS del commit (si el registro falla no sale ningún correo) y en otro hilo (el
 * SMTP tarda y no debe retrasar la respuesta del registro).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivationEmailListener {

    private final EmailSenderPort emailSender;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEvent event) {
        try {
            emailSender.sendActivation(event.email(), event.activationCode());
        } catch (RuntimeException e) {
            log.error("No se pudo enviar el email de activación a {}", event.email(), e);
        }
    }
}
