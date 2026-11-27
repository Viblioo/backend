package com.revibe.backend.infrastructure.adapters.input.rest.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

public record CreateEventRequest(

        @NotBlank @Size(max = 150)
        String title,

        @Size(max = 2000)
        String description,

        @NotNull @Positive
        Short categoryId,

        @NotBlank @Size(max = 200)
        String placeName,

        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")
        BigDecimal lat,

        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
        BigDecimal lon,

        @NotNull @Future
        Instant startsAt,

        @NotNull @Future
        Instant endsAt,

        @NotBlank @Pattern(regexp = "PUBLIC|PRIVATE", message = "visibility debe ser PUBLIC o PRIVATE")
        String visibility,

        @Positive
        Integer maxParticipants,

        @Size(max = 512)
        String coverKey, // opcional; el backend valida que pertenece al usuario

        @PositiveOrZero
        BigDecimal price, // si el evento puede ser de pago

        @Pattern(regexp = "[A-Z]{3}", message = "currency debe ser un código ISO de 3 letras")
        String currency

) {
    @AssertTrue(message = "el fin del evento debe ser posterior al inicio del evento")
    public boolean isDateRangeValid() {
        if (startsAt == null || endsAt == null)
            return true; // eso ya lo cubre @NotNull
        return endsAt.isAfter(startsAt);
    }

}
