package com.revibe.backend.support;

import com.revibe.backend.application.ports.output.EmailSenderPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Base de los tests que levantan la app entera contra PostgreSQL, Redis y Neo4j de docker-compose (hay que tenerlos
 * arrancados). Todos heredan la misma configuración para que Spring reutilice un único contexto.
 * <ul>
 * <li>Sin schedulers: que no procesen el outbox de la BD de desarrollo mientras corren los tests.</li>
 * <li>Emails simulados: no se envía nada por Brevo, y los tests pueden capturar los códigos que se habrían
 * enviado.</li>
 * </ul>
 */
@SpringBootTest(properties = "revibe.scheduling.enabled=false")
public abstract class IntegrationTest {

    @MockitoBean
    protected EmailSenderPort emailSender;
}
