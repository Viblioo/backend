package com.revibe.backend.infrastructure.adapters.input.rest;

import com.revibe.backend.application.ports.input.auth.ActivateAccountUseCase;
import com.revibe.backend.application.ports.input.auth.LoginCommand;
import com.revibe.backend.application.ports.input.auth.LoginUseCase;
import com.revibe.backend.application.ports.input.auth.LogoutUseCase;
import com.revibe.backend.application.ports.input.auth.PasswordResetUseCase;
import com.revibe.backend.application.ports.input.auth.RegisterCommand;
import com.revibe.backend.application.ports.input.auth.RegisterUseCase;
import com.revibe.backend.application.ports.input.auth.ResendActivationUseCase;
import com.revibe.backend.application.ports.input.auth.ResetPasswordCommand;
import com.revibe.backend.domain.exception.InvalidActivationCodeException;
import com.revibe.backend.infrastructure.adapters.input.rest.dto.EmailRequest;
import com.revibe.backend.infrastructure.adapters.input.rest.dto.LoginRequest;
import com.revibe.backend.infrastructure.adapters.input.rest.dto.LoginResponse;
import com.revibe.backend.infrastructure.adapters.input.rest.dto.RegisterRequest;
import com.revibe.backend.infrastructure.adapters.input.rest.dto.ResetPasswordRequest;
import com.revibe.backend.infrastructure.adapters.input.rest.dto.UserResponse;
import com.revibe.backend.infrastructure.adapters.input.rest.security.BearerToken;
import com.revibe.backend.infrastructure.adapters.input.rest.security.ClientIp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "Registro, activación, inicio y cierre de sesión y recuperación de contraseña")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegisterUseCase registerUseCase;
    private final ActivateAccountUseCase activateAccountUseCase;
    private final LoginUseCase loginUseCase;
    private final LogoutUseCase logoutUseCase;
    private final ResendActivationUseCase resendActivationUseCase;
    private final PasswordResetUseCase passwordResetUseCase;
    private final String activationSuccessUrl;
    private final String activationErrorUrl;

    public AuthController(RegisterUseCase registerUseCase,
            ActivateAccountUseCase activateAccountUseCase,
            LoginUseCase loginUseCase,
            LogoutUseCase logoutUseCase,
            ResendActivationUseCase resendActivationUseCase,
            PasswordResetUseCase passwordResetUseCase,
            @Value("${revibe.activation.success-url}")
            String activationSuccessUrl,
            @Value("${revibe.activation.error-url}")
            String activationErrorUrl) {
        this.registerUseCase = registerUseCase;
        this.activateAccountUseCase = activateAccountUseCase;
        this.loginUseCase = loginUseCase;
        this.logoutUseCase = logoutUseCase;
        this.resendActivationUseCase = resendActivationUseCase;
        this.passwordResetUseCase = passwordResetUseCase;
        this.activationSuccessUrl = activationSuccessUrl;
        this.activationErrorUrl = activationErrorUrl;
    }

    @Operation(summary = "Registrar usuario", description = "Crea el usuario en estado PENDING y le envía el email de activación.")
    @ApiResponse(responseCode = "201", description = "Usuario creado")
    @ApiResponse(responseCode = "400", description = "Datos no válidos", content = @Content(mediaType = "application/problem+json"))
    @ApiResponse(responseCode = "409", description = "Username o email ya registrado", content = @Content(mediaType = "application/problem+json"))
    @ApiResponse(responseCode = "429", description = "Demasiados registros desde esta IP", content = @Content(mediaType = "application/problem+json"))
    @SecurityRequirements
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody
    RegisterRequest request, HttpServletRequest http) {
        var command = new RegisterCommand(request.username(), request.email(), request.password(),
                request.firstName(), request.lastName(), request.birthDate(), ClientIp.of(http));

        var result = registerUseCase.register(command);

        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(result.user()));
    }

    @Operation(summary = "Iniciar sesión", description = "Comprueba usuario (username o email) y contraseña y devuelve "
            + "un token de sesión. La sesión caduca a los 30 días sin usarse: cada petición con el token reinicia ese plazo.")
    @ApiResponse(responseCode = "200", description = "Sesión creada")
    @ApiResponse(responseCode = "400", description = "Faltan campos", content = @Content(mediaType = "application/problem+json"))
    @ApiResponse(responseCode = "401", description = "Usuario o contraseña incorrectos", content = @Content(mediaType = "application/problem+json"))
    @ApiResponse(responseCode = "403", description = "Cuenta sin activar o suspendida (campo status)", content = @Content(mediaType = "application/problem+json"))
    @ApiResponse(responseCode = "429", description = "Demasiados intentos fallidos con esta cuenta o desde esta IP (cabecera Retry-After en segundos)", content = @Content(mediaType = "application/problem+json"))
    @SecurityRequirements
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody
    LoginRequest request, HttpServletRequest http) {
        var result = loginUseCase
                .login(new LoginCommand(request.username(), request.password(), ClientIp.of(http)));
        return LoginResponse.from(result);
    }

    @Operation(summary = "Reenviar email de activación", description = "Si el email es de una cuenta sin activar, "
            + "envía un enlace nuevo. Responde 202 exista o no el email, para no revelar qué emails están registrados.")
    @ApiResponse(responseCode = "202", description = "Petición aceptada")
    @ApiResponse(responseCode = "429", description = "Demasiadas peticiones para este email o desde esta IP", content = @Content(mediaType = "application/problem+json"))
    @SecurityRequirements
    @PostMapping("/activation/resend")
    public ResponseEntity<Void> resendActivation(@Valid @RequestBody
    EmailRequest request, HttpServletRequest http) {
        resendActivationUseCase.resend(request.email(), ClientIp.of(http));
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Pedir código para recuperar la contraseña", description = "Si el email es de una cuenta "
            + "activa, envía un código de 6 cifras que caduca en 15 minutos. Responde 202 exista o no el email.")
    @ApiResponse(responseCode = "202", description = "Petición aceptada")
    @ApiResponse(responseCode = "429", description = "Demasiadas peticiones para este email o desde esta IP", content = @Content(mediaType = "application/problem+json"))
    @SecurityRequirements
    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody
    EmailRequest request, HttpServletRequest http) {
        passwordResetUseCase.requestReset(request.email(), ClientIp.of(http));
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Cambiar la contraseña con el código", description = "Comprueba el código recibido por email "
            + "y cambia la contraseña. Cierra todas las sesiones abiertas del usuario. El código admite 5 intentos.")
    @ApiResponse(responseCode = "204", description = "Contraseña cambiada")
    @ApiResponse(responseCode = "400", description = "Código no válido o caducado, o contraseña nueva no válida", content = @Content(mediaType = "application/problem+json"))
    @SecurityRequirements
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody
    ResetPasswordRequest request) {
        passwordResetUseCase
                .resetPassword(new ResetPasswordCommand(request.email(), request.code(), request.newPassword()));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Cerrar sesión", description = "Invalida el token con el que se hace la petición. "
            + "Las sesiones de otros dispositivos siguen abiertas.")
    @ApiResponse(responseCode = "204", description = "Sesión cerrada")
    @ApiResponse(responseCode = "401", description = "Sin token o token no válido")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Parameter(hidden = true) @RequestHeader(HttpHeaders.AUTHORIZATION)
    String authorization) {
        // Si llega aquí el filtro ya ha validado el token (el endpoint requiere autenticación)
        BearerToken.from(authorization).ifPresent(logoutUseCase::logout);
        return ResponseEntity.noContent().build();
    }

    /**
     * Destino del botón del email. Activa la cuenta y redirige a la página de resultado (configurable, para poder
     * apuntar al frontend o a un deep link de la app más adelante).
     */
    @Operation(summary = "Activar cuenta", description = "Destino del botón del email. Pasa el usuario a ACTIVE y redirige a la página de "
            + "resultado. Pensado para abrirse en el navegador, no para llamarlo desde la app.")
    @ApiResponse(responseCode = "303", description = "Redirige a la página de éxito o de error (cabecera Location)")
    @SecurityRequirements
    @GetMapping("/activate")
    public ResponseEntity<Void> activate(
            @Parameter(description = "Código de activación recibido por email") @RequestParam(name = "code", required = false)
            String code) {
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
