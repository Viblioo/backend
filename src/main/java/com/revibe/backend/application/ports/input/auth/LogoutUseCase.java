package com.revibe.backend.application.ports.input.auth;

public interface LogoutUseCase {

    /** Invalida la sesión de ese token. Las demás sesiones del usuario (otros dispositivos) siguen abiertas. */
    void logout(String token);
}
