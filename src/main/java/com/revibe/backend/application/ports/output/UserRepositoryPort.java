package com.revibe.backend.application.ports.output;

import com.revibe.backend.domain.model.User;

public interface UserRepositoryPort {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /**
     * Guarda el usuario y devuelve el resultado con id y createdAt. Si la BD rechaza el insert por username/email
     * duplicado lanza UserAlreadyExistsException.
     */
    User save(User user);
}
