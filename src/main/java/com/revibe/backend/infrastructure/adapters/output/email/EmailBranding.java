package com.revibe.backend.infrastructure.adapters.output.email;

import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Marca que pinta EmailLayout: nombre, colores, logo y pie.
 *
 * @param logo PNG incrustado en el correo (CID) para que se vea aunque el cliente bloquee imágenes remotas. Si no
 *             existe, la cabecera muestra el nombre como texto.
 */
public record EmailBranding(String name, String primaryColor, String onPrimaryColor, Resource logo, String footer) {

    public static final String LOGO_CID = "brand-logo";

    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9a-fA-F]{6}$");

    public EmailBranding {
        // Los colores acaban dentro de atributos style: solo aceptamos #rrggbb
        if (!HEX_COLOR.matcher(primaryColor).matches() || !HEX_COLOR.matcher(onPrimaryColor).matches()) {
            throw new IllegalArgumentException("Los colores deben tener formato #rrggbb");
        }
    }

    public static EmailBranding revibe() {
        return new EmailBranding(
                "Revibe",
                "#6d28d9",
                "#ffffff",
                new ClassPathResource("email/logo.png"),
                "Has recibido este correo porque se ha usado esta dirección en Revibe.");
    }

    public boolean hasLogo() {
        return logo != null && logo.exists();
    }
}
