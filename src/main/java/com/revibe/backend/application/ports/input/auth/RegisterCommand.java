package com.revibe.backend.application.ports.input.auth;

import java.time.LocalDate;

/**
 * Datos de entrada del caso de uso de registro. No sabe nada de HTTP: el controlador lo construye a partir de
 * RegisterRequest.
 *
 * @param clientIp
 *            IP de quien se registra, para limitar registros masivos
 */
public record RegisterCommand(String username, String email, String rawPassword, String firstName, String lastName,
        LocalDate birthDate, String clientIp) {

    // Evita que la contraseña acabe en los logs si alguien hace log.info(command)
    @Override
    public String toString() {
        return "RegisterCommand[username=%s, email=%s]".formatted(username, email);
    }
}
