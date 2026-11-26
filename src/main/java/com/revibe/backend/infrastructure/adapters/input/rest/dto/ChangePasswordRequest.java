package com.revibe.backend.infrastructure.adapters.input.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body de POST /api/v1/users/me/password. */
public record ChangePasswordRequest(

        @NotBlank @Size(max = 256)
        String currentPassword,

        @NotBlank @Size(min = 8, max = 72, message = "Entre 8 y 72 caracteres")
        String newPassword) {

    @Override
    public String toString() {
        return "ChangePasswordRequest[]";
    }
}
