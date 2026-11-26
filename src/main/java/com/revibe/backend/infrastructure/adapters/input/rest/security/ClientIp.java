package com.revibe.backend.infrastructure.adapters.input.rest.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * IP del cliente para los límites de peticiones.
 * <p>
 * Usa getRemoteAddr(), no la cabecera X-Forwarded-For directamente: esa cabecera la puede inventar cualquiera y
 * saltarse el límite. Si la app va detrás de un proxy/balanceador de confianza, activar
 * {@code server.forward-headers-strategy: framework} y Spring pondrá aquí la IP real.
 */
public final class ClientIp {

    private ClientIp() {
    }

    public static String of(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
