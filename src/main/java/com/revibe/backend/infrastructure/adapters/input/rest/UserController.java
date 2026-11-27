package com.revibe.backend.infrastructure.adapters.input.rest;

import com.revibe.backend.application.ports.input.auth.ChangePasswordCommand;
import com.revibe.backend.application.ports.input.auth.ChangePasswordUseCase;
import com.revibe.backend.application.ports.input.user.GetCurrentUserUseCase;
import com.revibe.backend.infrastructure.adapters.input.rest.dto.ChangePasswordRequest;
import com.revibe.backend.infrastructure.adapters.input.rest.dto.UserResponse;
import com.revibe.backend.infrastructure.adapters.input.rest.security.BearerToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints del usuario de la sesión. El principal que deja OpaqueTokenAuthFilter es el UUID del usuario.
 */
@Tag(name = "Users", description = "Usuario de la sesión actual")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;

    @Operation(summary = "Usuario actual", description = "Datos del usuario dueño del token. Sirve también para "
            + "comprobar al abrir la app si el token guardado sigue siendo válido.")
    @ApiResponse(responseCode = "200", description = "Usuario de la sesión")
    @ApiResponse(responseCode = "401", description = "Sin token o token no válido")
    @GetMapping("/me")
    public UserResponse me(@Parameter(hidden = true) @AuthenticationPrincipal
    UUID userId) {
        return UserResponse.from(getCurrentUserUseCase.getCurrentUser(userId));
    }

    @Operation(summary = "Cambiar contraseña", description = "Pide la contraseña actual. Cierra las sesiones de los "
            + "demás dispositivos; esta sigue abierta.")
    @ApiResponse(responseCode = "204", description = "Contraseña cambiada")
    @ApiResponse(responseCode = "400", description = "Contraseña actual incorrecta o nueva no válida (campo errors)", content = @Content(mediaType = "application/problem+json"))
    @ApiResponse(responseCode = "429", description = "Demasiados intentos con la contraseña actual incorrecta", content = @Content(mediaType = "application/problem+json"))
    @PostMapping("/me/password")
    public ResponseEntity<Void> changePassword(@Parameter(hidden = true) @AuthenticationPrincipal
    UUID userId,
            @Parameter(hidden = true) @RequestHeader(HttpHeaders.AUTHORIZATION)
            String authorization,
            @Valid @RequestBody
            ChangePasswordRequest request) {
        // El filtro ya ha validado el token: aquí siempre está
        String token = BearerToken.from(authorization).orElseThrow();
        changePasswordUseCase.changePassword(
                new ChangePasswordCommand(userId, token, request.currentPassword(), request.newPassword()));
        return ResponseEntity.noContent().build();
    }
}
