package com.revibe.backend.infrastructure.adapters.output.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

class EmailLayoutTest {

    /** Sin logo para que el test no dependa de src/main/resources. */
    private static final EmailBranding BRANDING = new EmailBranding("Revibe", "#6d28d9", "#ffffff",
            new ByteArrayResource(new byte[0]) {
                @Override
                public boolean exists() {
                    return false;
                }
            }, "Pie de prueba");

    @Test
    void escapesHtmlInUserText() {
        var email = EmailLayout.with(BRANDING).paragraph("Hola <script>alert(1)</script> & adiós").render();

        assertThat(email.html()).doesNotContain("<script>");
        assertThat(email.html()).contains("&lt;script&gt;");
        assertThat(email.html()).contains("&amp; adiós");
    }

    @Test
    void plainTextVersionHasTheSameContent() {
        var email = EmailLayout.with(BRANDING)
                .title("Título")
                .paragraph("Párrafo")
                .button("Activar", "https://example.com/a?code=1")
                .code("123456")
                .render();

        assertThat(email.text())
                .contains("Título")
                .contains("Párrafo")
                .contains("Activar:\nhttps://example.com/a?code=1")
                .contains("123456")
                .endsWith("--\nPie de prueba\n");
    }

    @Test
    void rejectsNonHttpLinks() {
        assertThatThrownBy(() -> EmailLayout.with(BRANDING).button("Clic", "javascript:alert(1)"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void withoutLogoTheHeaderIsTheNameAndNothingIsEmbedded() {
        var email = EmailLayout.with(BRANDING).title("Hola").render();

        assertThat(email.inlineImages()).isEmpty();
        assertThat(email.html()).doesNotContain("cid:");
    }

    @Test
    void brandingRejectsColorsThatCouldInjectCss() {
        assertThatThrownBy(() -> new EmailBranding("X", "red;background:url(x)", "#ffffff", null, "pie"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
