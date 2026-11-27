package com.revibe.backend.application.ports.output.outbox;

/**
 * El grafo no está disponible ahora mismo (Neo4j caído, sin conexión, error transitorio). No es culpa del evento: quien
 * la reciba debe reintentar más tarde sin contarlo como intento fallido.
 */
public class GraphUnavailableException extends RuntimeException {

    public GraphUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
