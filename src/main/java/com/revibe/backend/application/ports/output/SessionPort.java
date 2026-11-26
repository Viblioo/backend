package com.revibe.backend.application.ports.output;

import java.util.UUID;

/** Crea sesiones (tokens opacos) para un usuario. */
public interface SessionPort {
    String create(UUID userId);
}
