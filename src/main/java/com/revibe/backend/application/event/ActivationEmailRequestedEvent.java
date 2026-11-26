package com.revibe.backend.application.event;

/**
 * Hay que enviar un email de activación (registro o reenvío). Si se publica dentro de una transacción, el email sale
 * cuando hace commit.
 */
public record ActivationEmailRequestedEvent(String email, String activationCode) {

    // El código da acceso a activar la cuenta: que no salga en los logs
    @Override
    public String toString() {
        return "ActivationEmailRequestedEvent[email=%s]".formatted(email);
    }
}
