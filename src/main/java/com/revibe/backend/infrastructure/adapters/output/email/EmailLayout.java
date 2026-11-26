package com.revibe.backend.infrastructure.adapters.output.email;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.util.HtmlUtils;

/**
 * Marco común de todos los emails: cabecera con la marca, tarjeta blanca de 600px y pie. Los bloques (título,
 * párrafo, botón...) se añaden en orden y cada uno escribe a la vez su versión HTML y su versión en texto plano.
 * <p>
 * HTML a mano con tablas y estilos en línea: es lo único que respetan todos los clientes de correo (Outlook incluido).
 * Todo el texto se escapa aquí, así que los métodos de EmailService pueden pasar datos del usuario sin preocuparse.
 */
public final class EmailLayout {

    private static final String FONT = "font-family:Arial,Helvetica,sans-serif;";
    private static final String BACKGROUND = "#f4f4f5";
    private static final String HEADING = "#111827";
    private static final String BODY = "#374151";
    private static final String MUTED = "#6b7280";

    private final EmailBranding branding;
    private final StringBuilder html = new StringBuilder();
    private final StringBuilder text = new StringBuilder();
    private String preheader = "";

    private EmailLayout(EmailBranding branding) {
        this.branding = branding;
    }

    public static EmailLayout with(EmailBranding branding) {
        return new EmailLayout(branding);
    }

    /** Texto que la bandeja de entrada muestra junto al asunto. No se ve dentro del email. */
    public EmailLayout preheader(String value) {
        this.preheader = value;
        return this;
    }

    public EmailLayout title(String value) {
        row("padding:0 0 16px;" + FONT + "font-size:24px;line-height:32px;font-weight:bold;color:" + HEADING + ";",
                esc(value));
        text.append(value).append("\n\n");
        return this;
    }

    public EmailLayout paragraph(String value) {
        row("padding:0 0 16px;" + FONT + "font-size:16px;line-height:24px;color:" + BODY + ";", esc(value));
        text.append(value).append("\n\n");
        return this;
    }

    /** Botón "a prueba de balas": el color va en el td, así Outlook lo pinta aunque ignore el padding del enlace. */
    public EmailLayout button(String label, String url) {
        html.append("""
                <tr><td align="center" style="padding:8px 0 24px;">
                  <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
                    <td align="center" bgcolor="%1$s" style="border-radius:8px;">
                      <a href="%2$s" target="_blank" style="display:inline-block;padding:14px 32px;%3$s\
                font-size:16px;font-weight:bold;color:%4$s;text-decoration:none;border-radius:8px;">%5$s</a>
                    </td>
                  </tr></table>
                </td></tr>
                """.formatted(branding.primaryColor(), escUrl(url), FONT, branding.onPrimaryColor(), esc(label)));
        text.append(label).append(":\n").append(url).append("\n\n");
        return this;
    }

    /** Enlace en texto para cuando el botón no funciona. Solo HTML: en texto plano el botón ya muestra la URL. */
    public EmailLayout fallbackLink(String url) {
        row("padding:0 0 16px;" + FONT + "font-size:13px;line-height:20px;color:" + MUTED + ";",
                "Si el botón no funciona, copia este enlace en el navegador:<br>"
                        + "<a href=\"%s\" target=\"_blank\" style=\"color:%s;word-break:break-all;\">%s</a>"
                                .formatted(escUrl(url), branding.primaryColor(), esc(url)));
        return this;
    }

    /** Texto pequeño y gris (avisos, "si no has sido tú..."). */
    public EmailLayout note(String value) {
        row("padding:0 0 16px;" + FONT + "font-size:13px;line-height:20px;color:" + MUTED + ";", esc(value));
        text.append(value).append("\n\n");
        return this;
    }

    public RenderedEmail render() {
        Map<String, org.springframework.core.io.Resource> inline = new LinkedHashMap<>();
        String header;
        if (branding.hasLogo()) {
            inline.put(EmailBranding.LOGO_CID, branding.logo());
            header = "<img src=\"cid:%s\" alt=\"%s\" height=\"40\" style=\"display:block;height:40px;border:0;\">"
                    .formatted(EmailBranding.LOGO_CID, esc(branding.name()));
        } else {
            header = "<span style=\"%sfont-size:26px;font-weight:bold;color:%s;\">%s</span>"
                    .formatted(FONT, branding.primaryColor(), esc(branding.name()));
        }

        String page = """
                <!doctype html>
                <html lang="es">
                <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width,initial-scale=1">
                <meta name="color-scheme" content="light">
                <title>%1$s</title>
                </head>
                <body style="margin:0;padding:0;background:%2$s;">
                <div style="display:none;max-height:0;overflow:hidden;opacity:0;">%3$s</div>
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" bgcolor="%2$s">
                  <tr><td align="center" style="padding:32px 16px;">
                    <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0"
                           style="width:100%%;max-width:600px;">
                      <tr><td align="center" style="padding:0 0 24px;">%4$s</td></tr>
                      <tr><td bgcolor="#ffffff" style="background:#ffffff;border-radius:12px;padding:40px 32px;">
                        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0">
                %5$s        </table>
                      </td></tr>
                      <tr><td align="center" style="padding:24px 16px 0;%6$sfont-size:12px;line-height:18px;color:%7$s;">
                        %8$s
                      </td></tr>
                    </table>
                  </td></tr>
                </table>
                </body>
                </html>
                """.formatted(esc(branding.name()), BACKGROUND, esc(preheader), header, html, FONT, MUTED,
                esc(branding.footer()));

        String plain = text + "--\n" + branding.footer() + "\n";

        return new RenderedEmail(page, plain, Map.copyOf(inline));
    }

    private void row(String style, String content) {
        html.append("<tr><td style=\"").append(style).append("\">").append(content).append("</td></tr>\n");
    }

    private static String esc(String value) {
        return HtmlUtils.htmlEscape(value, "UTF-8");
    }

    /** Solo enlaces http(s): evita que un javascript: o similar acabe en un href. */
    private static String escUrl(String url) {
        if (!url.startsWith("https://") && !url.startsWith("http://")) {
            throw new IllegalArgumentException("URL no permitida en un email: " + url);
        }
        return esc(url);
    }
}
