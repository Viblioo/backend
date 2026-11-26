package com.revibe.backend.domain.exception;

import com.revibe.backend.domain.model.UserStatus;
import lombok.Getter;

/**
 * Credenciales correctas pero la cuenta no puede iniciar sesión (sin verificar el email o suspendida). Solo se lanza
 * después de comprobar la contraseña, así que no revela nada a quien no la conozca.
 */
@Getter
public class AccountNotActiveException extends RuntimeException {

    private final UserStatus status;

    public AccountNotActiveException(UserStatus status) {
        super(switch (status) {
            case PENDING -> "Tienes que activar tu cuenta desde el email que te enviamos";
            case SUSPENDED -> "Tu cuenta está suspendida";
            default -> "La cuenta no está activa";
        });
        this.status = status;
    }
}
