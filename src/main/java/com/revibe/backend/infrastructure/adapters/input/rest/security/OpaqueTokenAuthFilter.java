package com.revibe.backend.infrastructure.adapters.input.rest.security;

import com.revibe.backend.application.ports.output.security.SessionPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lee "Authorization: Bearer <token>", lo resuelve contra las sesiones (lo que además renueva su TTL) y deja el userId
 * como principal. Sin token o con uno caducado no autentica: los endpoints protegidos responderán 401.
 */
@RequiredArgsConstructor
public class OpaqueTokenAuthFilter extends OncePerRequestFilter {

    private final SessionPort sessions;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        BearerToken.from(req.getHeader(HttpHeaders.AUTHORIZATION))
                .flatMap(sessions::resolve)
                .ifPresent(userId -> {
                    var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                });
        chain.doFilter(req, res);
    }
}
