package com.revibe.backend.infrastructure.adapters.output.email;

import com.revibe.backend.application.ports.output.EmailSenderPort;
import jakarta.mail.MessagingException;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Un metodo por email: cada uno junta bloques de EmailLayout y lo envía por SMTP (Brevo) con Spring Mail.
 */
@Component
public class EmailService implements EmailSenderPort {

    private final JavaMailSender mailSender;
    private final String from;
    private final String activationUrl;
    private final EmailBranding branding = EmailBranding.revibe();

    public EmailService(JavaMailSender mailSender,
            @Value("${revibe.mail.from}") String from,
            @Value("${revibe.mail.activation-url}") String activationUrl) {
        this.mailSender = mailSender;
        this.from = from;
        this.activationUrl = activationUrl;
    }

    @Override
    public void sendActivation(String to, String code) {
        String link = UriComponentsBuilder.fromUriString(activationUrl)
                .queryParam("code", code)
                .build()
                .toUriString();

        var email = EmailLayout.with(branding)
                .preheader("Confirma tu email para empezar a usar Revibe")
                .title("¡Bienvenido a Revibe!")
                .paragraph("Pulsa el botón para activar tu cuenta. El enlace caduca en 24 horas.")
                .button("Activar cuenta", link)
                .fallbackLink(link)
                .note("Si no has creado una cuenta, ignora este correo.")
                .render();

        send(to, "Activa tu cuenta de Revibe", email);
    }

    private void send(String to, String subject, RenderedEmail email) {
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(email.text(), email.html());
            // addInline siempre después de setText (lo exige MimeMessageHelper)
            for (var image : email.inlineImages().entrySet()) {
                helper.addInline(image.getKey(), image.getValue());
            }
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new MailSendException("No se pudo construir el email: " + subject, e);
        }
    }
}
