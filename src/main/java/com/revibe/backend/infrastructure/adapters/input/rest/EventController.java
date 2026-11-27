package com.revibe.backend.infrastructure.adapters.input.rest;

import com.revibe.backend.application.ports.input.event.CreateEventCommand;
import com.revibe.backend.application.ports.input.event.CreateEventUseCase;
import com.revibe.backend.domain.model.EventVisibility;
import com.revibe.backend.infrastructure.adapters.input.rest.dto.CreateEventRequest;
import com.revibe.backend.infrastructure.adapters.input.rest.dto.CreateEventResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Events", description = "Creación y gestión de eventos")
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final CreateEventUseCase createEventUseCase;

    @Operation(summary = "Crear evento", description = "Crea un evento activo para el usuario autenticado.")
    @ApiResponse(responseCode = "201", description = "Evento creado")
    @ApiResponse(responseCode = "400", description = "Datos del evento no válidos", content = @Content(mediaType = "application/problem+json"))
    @ApiResponse(responseCode = "401", description = "Sin token o token no válido")
    @PostMapping
    public ResponseEntity<CreateEventResponse> create(@Valid @RequestBody
    CreateEventRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal
            UUID userId) {
        var command = new CreateEventCommand(userId, request.title(), request.description(), request.categoryId(),
                request.placeName(), request.lat(), request.lon(), request.startsAt(), request.endsAt(),
                EventVisibility.valueOf(request.visibility()), request.maxParticipants(), request.coverKey(),
                request.currency());

        var result = createEventUseCase.create(command);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CreateEventResponse.from(result.event(), result.coverUrl()));
    }
}
