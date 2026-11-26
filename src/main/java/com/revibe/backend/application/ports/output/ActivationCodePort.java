package com.revibe.backend.application.ports.output;

import java.util.Optional;
import java.util.UUID;

/**
 * Códigos de activación de cuenta de un solo uso y con caducidad.
 */
public interface ActivationCodePort {

    /** Genera un código nuevo para el usuario y lo devuelve en claro (solo se guarda su hash). */
    String create(UUID userId);

    /** Devuelve el usuario del código y lo invalida. Vacío si no existe o ha caducado. */
    Optional<UUID> consume(String code);
}
