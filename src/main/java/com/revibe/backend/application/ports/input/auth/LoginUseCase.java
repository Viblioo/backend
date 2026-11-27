package com.revibe.backend.application.ports.input.auth;

public interface LoginUseCase {

    /**
     * Comprueba usuario (username o email) y contraseña y abre una sesión.
     *
     * @throws com.revibe.backend.domain.exception.InvalidCredentialsException
     *             si el usuario no existe o la contraseña no coincide
     * @throws com.revibe.backend.domain.exception.AccountNotActiveException
     *             si las credenciales son correctas pero la cuenta está PENDING o SUSPENDED
     * @throws com.revibe.backend.domain.exception.TooManyRequestsException
     *             si la cuenta o la IP han superado el límite de intentos fallidos
     */
    LoginResult login(LoginCommand command);
}
