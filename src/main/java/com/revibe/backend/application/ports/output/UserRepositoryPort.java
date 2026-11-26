package com.revibe.backend.application.ports.output;

import com.revibe.backend.domain.model.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findById(UUID id);

    /** Búsqueda sin distinguir mayúsculas (la columna es citext). */
    Optional<User> findByUsername(String username);

    /** Búsqueda sin distinguir mayúsculas (la columna es citext). */
    Optional<User> findByEmail(String email);

    /**
     * Guarda el usuario y devuelve el resultado con id y createdAt. Si la BD rechaza el insert por username/email
     * duplicado lanza UserAlreadyExistsException.
     */
    User save(User user);

    /**
     * Pasa el usuario de PENDING a ACTIVE y marca el email como verificado. Devuelve false si no estaba PENDING.
     */
    boolean activate(UUID userId, Instant verifiedAt);

    void updatePasswordHash(UUID userId, String passwordHash);

    /**
     * Ids de usuarios ACTIVE con id mayor que {@code afterId}, ordenados por id (paginación por clave). Para recorrer
     * todos, empezar con el UUID 00000000-... y pasar el último id de cada página.
     */
    List<UUID> findActiveIdsAfter(UUID afterId, int limit);
}
