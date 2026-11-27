package com.revibe.backend.infrastructure.adapters.output.email;

import com.revibe.backend.application.event.ActivationEmailRequestedEvent;
import com.revibe.backend.application.event.PasswordResetRequestedEvent;
import com.revibe.backend.application.ports.output.auth.EmailSenderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Envía los emails de auth en otro hilo (el SMTP tarda y no debe retrasar la respuesta; en los flujos de "olvidé mi
 * contraseña" además así la respuesta tarda lo mismo exista o no el email).
 * <p>
 * AFTER_COMMIT: si el evento se publica dentro de una transacción (registro), el email solo sale si hace commit.
 * fallbackExecution: si se publica sin transacción (reenvío, recuperar contraseña), sale igualmente.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthEmailListener {

    private final EmailSenderPort emailSender;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onActivationEmailRequested(ActivationEmailRequestedEvent event) {
        try {
            emailSender.sendActivation(event.email(), event.activationCode());
        } catch (RuntimeException e) {
            log.error("No se pudo enviar el email de activación a {}", event.email(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        try {
            emailSender.sendPasswordReset(event.email(), event.code());
        } catch (RuntimeException e) {
            log.error("No se pudo enviar el email de recuperación a {}", event.email(), e);
        }
    }
}
