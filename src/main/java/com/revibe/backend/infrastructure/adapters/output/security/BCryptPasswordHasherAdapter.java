package com.revibe.backend.infrastructure.adapters.output.security;

import com.revibe.backend.application.ports.output.PasswordHasherPort;
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
}
