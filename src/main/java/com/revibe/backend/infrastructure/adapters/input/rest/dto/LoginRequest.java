package com.revibe.backend.infrastructure.adapters.input.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body de POST /api/v1/auth/login. Los límites de tamaño solo evitan bodies enormes; las reglas reales las aplica el
 * caso de uso.
 */
public record LoginRequest(

        @Schema(description = "Username o email", example = "maria_lopez") @NotBlank @Size(max = 254)
        String username,

        @NotBlank @Size(max = 256)
        String password) {

    @Override
    public String toString() {
        return "LoginRequest[username=%s]".formatted(username);
    }
}
