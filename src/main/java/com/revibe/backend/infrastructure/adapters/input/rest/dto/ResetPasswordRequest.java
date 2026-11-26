package com.revibe.backend.infrastructure.adapters.input.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Body de POST /api/v1/auth/password-reset/confirm. */
public record ResetPasswordRequest(

        @NotBlank @Email @Size(max = 254)
        String email,

        @NotBlank @Pattern(regexp = "^\\s*\\d{6}\\s*$", message = "El código tiene 6 cifras")
        String code,

        @NotBlank @Size(min = 8, max = 72, message = "Entre 8 y 72 caracteres")
        String newPassword) {

    @Override
    public String toString() {
        return "ResetPasswordRequest[email=%s]".formatted(email);
    }
}
