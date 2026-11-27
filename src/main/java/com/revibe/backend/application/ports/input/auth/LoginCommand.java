package com.revibe.backend.application.ports.input.auth;

/**
 * @param login
 *            username o email
 * @param clientIp
 *            IP desde la que se intenta, para el límite de intentos por IP
 */
public record LoginCommand(String login, String rawPassword, String clientIp) {

    // Que la contraseña no acabe en los logs
    @Override
    public String toString() {
        return "LoginCommand[login=%s, clientIp=%s]".formatted(login, clientIp);
    }
}
