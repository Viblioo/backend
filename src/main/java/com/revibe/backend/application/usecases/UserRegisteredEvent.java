package com.revibe.backend.application.usecases;

/**
 * Se publica al registrar un usuario. El email de activación se envía cuando la transacción hace commit.
 */
public record UserRegisteredEvent(String email, String activationCode) {

    // El código da acceso a activar la cuenta: que no salga en los logs
    @Override
    public String toString() {
        return "UserRegisteredEvent[email=%s]".formatted(email);
    }
}
