package com.revibe.backend.application.ports.input.auth;

public interface ResendActivationUseCase {

    /**
     * Envía un email de activación nuevo si el email es de una cuenta PENDING. Si no existe o ya está activa no hace
     * nada y no lo dice: así no se puede averiguar qué emails están registrados.
     */
    void resend(String email, String clientIp);
}
