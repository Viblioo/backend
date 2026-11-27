package com.revibe.backend.application.ports.input.auth;

/**
 * Puerto de entrada: lo que el exterior (p. ej. el controlador REST) puede pedir para registrar un usuario.
 */
public interface RegisterUseCase {

    RegisterResult register(RegisterCommand command);
}
