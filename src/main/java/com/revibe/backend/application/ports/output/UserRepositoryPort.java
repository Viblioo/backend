package com.revibe.backend.application.ports.output;

import com.revibe.backend.domain.model.User;
import java.time.Instant;
import java.util.UUID;

public interface UserRepositoryPort {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /**
     * Guarda el usuario y devuelve el resultado con id y createdAt. Si la BD rechaza el insert por username/email
     * duplicado lanza UserAlreadyExistsException.
     */
    User save(User user);

    /**
     * Pasa el usuario de PENDING a ACTIVE y marca el email como verificado. Devuelve false si no estaba PENDING.
     */
    boolean activate(UUID userId, Instant verifiedAt);
}
