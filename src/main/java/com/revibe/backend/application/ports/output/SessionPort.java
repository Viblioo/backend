package com.revibe.backend.application.ports.output;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Sesiones con token opaco. El token solo lo conoce el cliente; el almacén guarda su hash.
 */
public interface SessionPort {

    /** Crea una sesión nueva para el usuario y devuelve el token en claro. */
    String create(UUID userId);

    /**
     * Devuelve el usuario del token y renueva su caducidad (sesión deslizante: mientras se use, no caduca). Vacío si no
     * existe o ha caducado.
     */
    Optional<UUID> resolve(String token);

    /** Invalida el token (logout). */
    void revoke(String token);

    /** Invalida todas las sesiones del usuario (contraseña recuperada, cuenta suspendida...). */
    void revokeAll(UUID userId);

    /** Invalida todas las sesiones del usuario menos la de {@code keepToken} (cambio de contraseña). */
    void revokeOthers(UUID userId, String keepToken);

    /** Cuánto dura una sesión sin usarse. */
    Duration ttl();
}
