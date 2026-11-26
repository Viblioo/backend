package com.revibe.backend.infrastructure.adapters.input.rest;

import com.revibe.backend.application.usecases.RegisterCommand;
import com.revibe.backend.application.usecases.RegisterUseCase;
import com.revibe.backend.infrastructure.adapters.input.rest.dto.RegisterRequest;
import com.revibe.backend.infrastructure.adapters.input.rest.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterUseCase registerUseCase;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody
    RegisterRequest request) {
        var command = new RegisterCommand(request.username(), request.email(), request.password(),
                request.firstName(), request.lastName(), request.birthDate());

        var result = registerUseCase.register(command);

        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(result.user(), result.token()));
    }
}
