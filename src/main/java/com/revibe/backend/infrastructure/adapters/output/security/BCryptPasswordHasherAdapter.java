package com.revibe.backend.infrastructure.adapters.output.security;

import com.revibe.backend.application.ports.output.PasswordHasherPort;
import com.revibe.backend.domain.model.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Adapta el PasswordEncoder de Spring Security (BCrypt 12, definido en SecurityConfig) al puerto. */
@Component
@RequiredArgsConstructor
public class BCryptPasswordHasherAdapter implements PasswordHasherPort {

    private final PasswordEncoder passwordEncoder;

    @Override
    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String hash) {
        // BCrypt rechaza más de 72 bytes con una excepción; para el puerto simplemente no coincide
        if (rawPassword == null || PasswordPolicy.exceedsMaxBytes(rawPassword)) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, hash);
    }
}
