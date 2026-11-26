package com.revibe.backend.infrastructure.adapters.input.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body de los endpoints que solo piden un email (reenviar activación, pedir código de recuperación). */
public record EmailRequest(@NotBlank @Email @Size(max = 254)
String email) {
}
