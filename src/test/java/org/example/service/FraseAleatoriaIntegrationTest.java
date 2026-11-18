package org.example.service;

import org.example.dto.EstadoAnimoDTO;
import org.example.dto.EstadoAnimoRequest;
import org.example.dto.FraseDTO;
import org.example.dto.FraseRequest;
import org.example.exception.BadRequestException;
import org.example.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * GET /api/frases/random (hallazgo 9 de la auditoría): el cliente debe poder distinguir
 * "el ánimo no existe" de "no quedan frases" y la lista de excluidos tiene tope.
 */
@SpringBootTest
@ActiveProfiles("test")
class FraseAleatoriaIntegrationTest {

    @Autowired
    private IEstadoAnimoService estadoService;

    @Autowired
    private IFraseService fraseService;

    private EstadoAnimoDTO crearEstado() {
        String codigo = "r-" + UUID.randomUUID().toString().substring(0, 8);
        return estadoService.guardar(new EstadoAnimoRequest(codigo, "😊", null, null, null,
                "#FFD700", "#FFA500", "Poppins", "float", Map.of("es", "Feliz")));
    }

    @Test
    void animoInexistente_LanzaNotFoundDelEstado() {
        assertThatThrownBy(() -> fraseService.obtenerFraseAleatoria("no-existe", List.of()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("error.estadoanimo.codigo.notfound")
                .satisfies(ex -> assertThat(((ResourceNotFoundException) ex).getArgs()).containsExactly("no-existe"));
    }

    @Test
    void estadoSinFrases_LanzaNotFoundSinFrases() {
        EstadoAnimoDTO e = crearEstado();

        assertThatThrownBy(() -> fraseService.obtenerFraseAleatoria(e.codigo(), List.of()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("error.frase.sinfrases");
    }

    @Test
    void todasExcluidas_LanzaNotFoundAgotadas() {
        EstadoAnimoDTO e = crearEstado();
        FraseDTO f1 = fraseService.guardar(new FraseRequest(Map.of("es", "Uno"), e.id()));
        FraseDTO f2 = fraseService.guardar(new FraseRequest(Map.of("es", "Dos"), e.id()));

        assertThatThrownBy(() -> fraseService.obtenerFraseAleatoria(e.codigo(), List.of(f1.id(), f2.id())))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("error.frase.agotadas");
    }

    @Test
    void devuelveUnaFraseNoExcluida() {
        EstadoAnimoDTO e = crearEstado();
        FraseDTO f1 = fraseService.guardar(new FraseRequest(Map.of("es", "Uno"), e.id()));
        FraseDTO f2 = fraseService.guardar(new FraseRequest(Map.of("es", "Dos"), e.id()));

        assertThat(fraseService.obtenerFraseAleatoria(e.codigo(), List.of(f1.id())).id()).isEqualTo(f2.id());
        assertThat(fraseService.obtenerFraseAleatoria(e.codigo().toUpperCase(), List.of()).id()).isIn(f1.id(), f2.id());
    }

    @Test
    void excluidosConNulos_LosIgnora() {
        // "?excluidos=1,,2" llega como [1, null, 2].
        EstadoAnimoDTO e = crearEstado();
        FraseDTO f1 = fraseService.guardar(new FraseRequest(Map.of("es", "Uno"), e.id()));
        FraseDTO f2 = fraseService.guardar(new FraseRequest(Map.of("es", "Dos"), e.id()));

        List<Long> excluidos = new ArrayList<>(Arrays.asList(f1.id(), null));
        assertThat(fraseService.obtenerFraseAleatoria(e.codigo(), excluidos).id()).isEqualTo(f2.id());
        assertThat(fraseService.obtenerFraseAleatoria(e.codigo(), Collections.singletonList(null)).id())
                .isIn(f1.id(), f2.id());
    }

    @Test
    void masDe500Excluidos_SeRechaza() {
        EstadoAnimoDTO e = crearEstado();
        fraseService.guardar(new FraseRequest(Map.of("es", "Uno"), e.id()));
        // IDs negativos: nunca coinciden con una frase real, así el tope se prueba aislado.
        List<Long> excluidos = LongStream.rangeClosed(-501, -1).boxed().toList();

        assertThatThrownBy(() -> fraseService.obtenerFraseAleatoria(e.codigo(), excluidos))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("error.excluidos.size")
                .satisfies(ex -> assertThat(((BadRequestException) ex).getArgs()).containsExactly(500));

        assertThat(fraseService.obtenerFraseAleatoria(e.codigo(), excluidos.subList(0, 500))).isNotNull();
    }
}
