package com.revibe.backend.application.ports.input;

public interface ChangePasswordUseCase {

    /**
     * Cambia la contraseña comprobando la actual. Cierra las demás sesiones del usuario; la de {@code currentToken}
     * sigue abierta.
     */
    void changePassword(ChangePasswordCommand command);
}
