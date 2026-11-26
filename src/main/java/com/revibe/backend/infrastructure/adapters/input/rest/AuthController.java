package com.revibe.backend.infrastructure.adapters.input.rest;

import com.revibe.backend.application.usecases.ActivateAccountUseCase;
import com.revibe.backend.application.usecases.RegisterCommand;
import com.revibe.backend.application.usecases.RegisterUseCase;
import com.revibe.backend.domain.exception.InvalidActivationCodeException;
import com.revibe.backend.infrastructure.adapters.input.rest.dto.RegisterRequest;
import com.revibe.backend.infrastructure.adapters.input.rest.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "Registro y activación de cuentas")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegisterUseCase registerUseCase;
    private final ActivateAccountUseCase activateAccountUseCase;
    private final String activationSuccessUrl;
    private final String activationErrorUrl;

    public AuthController(RegisterUseCase registerUseCase,
            ActivateAccountUseCase activateAccountUseCase,
            @Value("${revibe.activation.success-url}") String activationSuccessUrl,
            @Value("${revibe.activation.error-url}") String activationErrorUrl) {
        this.registerUseCase = registerUseCase;
        this.activateAccountUseCase = activateAccountUseCase;
        this.activationSuccessUrl = activationSuccessUrl;
        this.activationErrorUrl = activationErrorUrl;
    }

    @Operation(summary = "Registrar usuario",
            description = "Crea el usuario en estado PENDING y le envía el email de activación.")
    @ApiResponse(responseCode = "201", description = "Usuario creado")
    @ApiResponse(responseCode = "400", description = "Datos no válidos",
            content = @Content(mediaType = "application/problem+json"))
    @ApiResponse(responseCode = "409", description = "Username o email ya registrado",
            content = @Content(mediaType = "application/problem+json"))
    @SecurityRequirements
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody
    RegisterRequest request) {
        var command = new RegisterCommand(request.username(), request.email(), request.password(),
                request.firstName(), request.lastName(), request.birthDate());

        var result = registerUseCase.register(command);

        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(result.user()));
    }

    /**
     * Destino del botón del email. Activa la cuenta y redirige a la página de resultado (configurable, para poder
     * apuntar al frontend o a un deep link de la app más adelante).
     */
    @Operation(summary = "Activar cuenta",
            description = "Destino del botón del email. Pasa el usuario a ACTIVE y redirige a la página de "
                    + "resultado. Pensado para abrirse en el navegador, no para llamarlo desde la app.")
    @ApiResponse(responseCode = "303",
            description = "Redirige a la página de éxito o de error (cabecera Location)")
    @SecurityRequirements
    @GetMapping("/activate")
    public ResponseEntity<Void> activate(
            @Parameter(description = "Código de activación recibido por email")
            @RequestParam(name = "code", required = false) String code) {
        String target;
        try {
            activateAccountUseCase.activate(code);
            target = activationSuccessUrl;
        } catch (InvalidActivationCodeException ex) {
            target = activationErrorUrl;
        }
        return ResponseEntity.status(HttpStatus.SEE_OTHER).location(URI.create(target)).build();
    }
}
