package com.revibe.backend.infrastructure.adapters.input.config;

public final class SecurityHeaders {

    private SecurityHeaders() {
    }

    /** API pura: no carga nada ni puede ir dentro de un frame. */
    public static final String CONTENT_SECURITY_POLICY = "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'";

    /**
     * Solo para Swagger UI: carga su JS/CSS del propio servidor. 'unsafe-inline' en estilos porque Swagger UI los
     * inyecta en línea.
     */
    public static final String SWAGGER_CONTENT_SECURITY_POLICY = "default-src 'self'; script-src 'self'; "
            + "style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; "
            + "frame-ancestors 'none'; base-uri 'self'; form-action 'self'";

    /** Desactiva funciones del dispositivo que una API nunca necesita. */
    public static final String PERMISSIONS_POLICY = "camera=(), microphone=(), geolocation=(), payment=(), usb=()";
}
