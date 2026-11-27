package com.revibe.backend.application.ports.input.auth;

import com.revibe.backend.domain.model.User;

/** Resultado del registro: el usuario creado. */
public record RegisterResult(User user) {

    // Evita que el token acabe en los logs si alguien hace log.info(result)
    @Override
    public String toString() {
        return "RegisterResult[userId=%s]".formatted(user.getId());
    }
}
