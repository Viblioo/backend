package com.revibe.backend.infrastructure.adapters.input.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Body de POST /api/v1/auth/register.
 */
public record RegisterRequest(

        @NotBlank @Pattern(regexp = "^[a-zA-Z0-9_.]{3,30}$", message = "Entre 3 y 30 caracteres: letras, números, '_' o '.'")
        String username,

        @NotBlank @Email @Size(max = 254)
        String email,

        @NotBlank @Size(min = 8, max = 72, message = "Entre 8 y 72 caracteres")
        String password,

        @NotBlank @Size(max = 50)
        String firstName,

        @Size(max = 80)
        String lastName,

        @NotNull @Past
        LocalDate birthDate) {

    // Evita que la contraseña acabe en los logs si alguien hace log.info(register)
    @Override
    public String toString() {
        return "RegisterRequest[username=%s, email=%s]".formatted(username, email);
    }
}
