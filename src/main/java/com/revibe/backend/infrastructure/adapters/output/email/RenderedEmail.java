package com.revibe.backend.infrastructure.adapters.output.email;

import java.util.Map;
import org.springframework.core.io.Resource;

/**
 * Resultado de EmailLayout: versión HTML, versión texto plano e imágenes a incrustar (Content-ID -> recurso).
 */
public record RenderedEmail(String html, String text, Map<String, Resource> inlineImages) {
}
