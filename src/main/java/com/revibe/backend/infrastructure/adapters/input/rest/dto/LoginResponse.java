package com.revibe.backend.infrastructure.adapters.input.rest.dto;

import com.revibe.backend.application.ports.input.auth.LoginResult;

/**
 * @param token
 *            enviarlo en cada petición como "Authorization: Bearer &lt;token&gt;"
 * @param expiresIn
 *            segundos hasta que caduca si no se usa; cada petición con el token lo vuelve a poner a este valor
 */
public record LoginResponse(String token, String tokenType, long expiresIn, UserResponse user) {

    public static LoginResponse from(LoginResult result) {
        return new LoginResponse(result.token(), "Bearer", result.expiresIn().toSeconds(),
                UserResponse.from(result.user()));
    }

    @Override
    public String toString() {
        return "LoginResponse[userId=%s]".formatted(user.id());
    }
}
