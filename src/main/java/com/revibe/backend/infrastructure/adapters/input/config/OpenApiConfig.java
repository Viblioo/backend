package com.revibe.backend.infrastructure.adapters.input.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación OpenAPI. JSON en /v3/api-docs y Swagger UI en /swagger-ui.html.
 * <p>
 * Por defecto todos los endpoints piden token (como en SecurityConfig). Los públicos lo indican con
 * {@code @SecurityRequirements()} vacío en el controller.
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI revibeOpenApi(@Value("${spring.application.name}") String appName) {
        return new OpenAPI()
                .info(new Info()
                        .title("Revibe API")
                        .version("v1")
                        .description("""
                                API REST de Revibe: planes sociales, gastos compartidos, álbumes de eventos y chat.

                                **Autenticación:** token opaco de sesión en la cabecera \
                                `Authorization: Bearer <token>`. Pulsa *Authorize* y pega solo el token.

                                Los errores siguen RFC 9457 (`application/problem+json`).
                                """))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .description("Token de sesión opaco (no es un JWT)")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
