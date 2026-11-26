package com.revibe.backend.application.ports.input;

public interface PasswordResetUseCase {

    /**
     * Envía un código de recuperación si el email es de una cuenta ACTIVE. Si no, no hace nada y no lo dice (no se debe
     * poder averiguar qué emails están registrados).
     */
    void requestReset(String email, String clientIp);

    /**
     * Cambia la contraseña con el código recibido y cierra todas las sesiones del usuario.
     *
     * @throws com.revibe.backend.domain.exception.InvalidPasswordResetCodeException
     *             si el código no es válido (o el email no tiene código)
     */
    void resetPassword(ResetPasswordCommand command);
}
