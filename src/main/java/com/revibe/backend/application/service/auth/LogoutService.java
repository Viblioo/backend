package com.revibe.backend.application.service.auth;

import com.revibe.backend.application.ports.input.auth.LogoutUseCase;
import com.revibe.backend.application.ports.output.security.SessionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutUseCase {

    private final SessionPort sessions;

    @Override
    public void logout(String token) {
        sessions.revoke(token);
    }
}
