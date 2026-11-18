package org.example.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validaciones de formato del request de estado de ánimo (hallazgos 8 y 15 de la auditoría).
 */
class EstadoAnimoRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void crearValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private static EstadoAnimoRequest request(String codigo, String iconUrl, String musicaUrl, String imagenUrl) {
        return new EstadoAnimoRequest(codigo, "😊", iconUrl, musicaUrl, imagenUrl, "#FFD700", "#FFA500",
                "Poppins", "float", Map.of("es", "Feliz"));
    }

    private static Set<String> camposInvalidos(EstadoAnimoRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(java.util.stream.Collectors.toSet());
    }

    @Test
    void requestValido_NoTieneErrores() {
        assertThat(camposInvalidos(request("feliz",
                "https://s3.us-west-1.wasabisys.com/b/tonin-api/imagenes/a.png",
                "http://cdn.ejemplo.com/musica.mp3",
                null))).isEmpty();
    }

    @Test
    void codigoDeMasDe50Caracteres_EsInvalido() {
        assertThat(camposInvalidos(request("a".repeat(51), null, null, null))).containsExactly("codigo");
        assertThat(camposInvalidos(request("a".repeat(50), null, null, null))).isEmpty();
    }

    @Test
    void urlsQueNoSonHttp_SonInvalidas() {
        for (String url : new String[]{"javascript:alert(1)", "data:image/png;base64,AAAA", "ftp://x.com/a.png",
                "https://", "https://x.com/a b.png", "/imagenes/a.png"}) {
            assertThat(camposInvalidos(request("feliz", url, url, url)))
                    .as(url)
                    .containsExactlyInAnyOrder("iconUrl", "musicaUrl", "imagenUrl");
        }
    }
}
