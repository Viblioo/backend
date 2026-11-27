package com.revibe.backend.infrastructure.adapters.input.rest;

import com.revibe.backend.domain.exception.*;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Convierte excepciones en respuestas RFC 9457 (ProblemDetail, application/problem+json). Heredar de
 * ResponseEntityExceptionHandler hace que los errores estándar de Spring (JSON mal formado, método no permitido...)
 * también salgan como ProblemDetail.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ProblemDetail handleUserAlreadyExists(UserAlreadyExistsException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Usuario ya existente");
        pd.setProperty("field", ex.getField().name().toLowerCase(java.util.Locale.ROOT));
        return pd;
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        pd.setTitle("Credenciales incorrectas");
        return pd;
    }

    /** 403 con "status" (pending / suspended) para que la app sepa qué pantalla enseñar. */
    @ExceptionHandler(AccountNotActiveException.class)
    public ProblemDetail handleAccountNotActive(AccountNotActiveException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        pd.setTitle("Cuenta no activa");
        pd.setProperty("status", ex.getStatus().name().toLowerCase(Locale.ROOT));
        return pd;
    }

    /** 429 con Retry-After (segundos), la cabecera estándar para decirle al cliente cuándo reintentar. */
    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ProblemDetail> handleTooManyRequests(TooManyRequestsException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        pd.setTitle("Demasiados intentos");
        pd.setProperty("retryAfterSeconds", ex.getRetryAfter().toSeconds());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfter().toSeconds()))
                .body(pd);
    }

    @ExceptionHandler(InvalidPasswordResetCodeException.class)
    public ProblemDetail handleInvalidPasswordResetCode(InvalidPasswordResetCodeException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Código no válido");
        return pd;
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("No encontrado");
        return pd;
    }

    @ExceptionHandler(InvalidUserDataException.class)
    public ProblemDetail handleInvalidUserData(InvalidUserDataException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Datos no válidos");
        pd.setTitle("Validación fallida");
        pd.setProperty("errors", Map.of(ex.getField(), ex.getMessage()));
        return pd;
    }

    @ExceptionHandler(InvalidEventDataException.class)
    public ProblemDetail handleInvalidEventData(InvalidEventDataException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Datos del evento no válidos");
        pd.setTitle("Validación fallida");
        pd.setProperty("errors", Map.of(ex.getField(), ex.getMessage()));
        return pd;
    }

    /** Errores de @Valid: devolvemos un mapa campo -> mensaje para que la app los pinte. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            @NonNull
            HttpHeaders headers,
            @NonNull
            HttpStatusCode status,
            @NonNull
            WebRequest request) {
        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() == null ? "Valor no válido" : fe.getDefaultMessage(),
                        (first, second) -> first));

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Datos no válidos");
        pd.setTitle("Validación fallida");
        pd.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(pd);
    }
}
