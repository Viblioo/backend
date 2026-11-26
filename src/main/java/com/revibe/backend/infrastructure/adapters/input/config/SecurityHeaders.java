package com.revibe.backend.infrastructure.adapters.input.config;

public final class SecurityHeaders {

    private SecurityHeaders() {
    }

    /** API pura: no carga nada ni puede ir dentro de un frame. */
    public static final String CONTENT_SECURITY_POLICY = "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'";

    /** Desactiva funciones del dispositivo que una API nunca necesita. */
    public static final String PERMISSIONS_POLICY = "camera=(), microphone=(), geolocation=(), payment=(), usb=()";
}
