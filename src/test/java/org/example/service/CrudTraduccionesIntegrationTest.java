package org.example.service;

import org.example.dto.EstadoAnimoDTO;
import org.example.dto.EstadoAnimoRequest;
import org.example.dto.FraseDTO;
import org.example.dto.FraseRequest;
import org.example.exception.BadRequestException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de integración (H2) del ciclo de vida de frases y estados con traducciones.
 * Sin @Transactional a propósito: reproduce el comportamiento real de una request,
 * donde cada llamada al service corre en su propia transacción.
 */
@SpringBootTest
@ActiveProfiles("test")
class CrudTraduccionesIntegrationTest {

    @Autowired
    private IEstadoAnimoService estadoService;

    @Autowired
    private IFraseService fraseService;

    @AfterEach
    void limpiarLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    private static String codigoUnico() {
        return "e-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static EstadoAnimoRequest estado(String codigo, Map<String, String> traducciones) {
        return new EstadoAnimoRequest(codigo, "😊", null, null, null, "#FFD700", "#FFA500", "Poppins", "float", traducciones);
    }

    private EstadoAnimoDTO crearEstado() {
        return estadoService.guardar(estado(codigoUnico(), Map.of("es", "Feliz")));
    }

    // --- Hallazgo 1: editar no debe fallar por la restricción única (owner, locale) ---

    @Test
    void actualizarFrase_ModificaAgregaYQuitaTraducciones() {
        EstadoAnimoDTO e = crearEstado();
        FraseDTO creada = fraseService.guardar(new FraseRequest(Map.of("es", "Hola", "en", "Hello"), e.id()));

        FraseDTO editada = fraseService.actualizar(creada.id(), new FraseRequest(Map.of("es", "Hola editado"), e.id()));

        assertThat(editada.traducciones()).containsExactlyEntriesOf(Map.of("es", "Hola editado"));
        assertThat(fraseService.obtenerPorId(creada.id()).traducciones())
                .containsExactlyEntriesOf(Map.of("es", "Hola editado"));

        FraseDTO conIngles = fraseService.actualizar(creada.id(),
                new FraseRequest(Map.of("es", "Hola editado", "en", "Hi"), e.id()));
        assertThat(conIngles.traducciones()).containsEntry("es", "Hola editado").containsEntry("en", "Hi").hasSize(2);
    }

    @Test
    void actualizarFrase_PuedeCambiarDeEstado() {
        EstadoAnimoDTO e1 = crearEstado();
        EstadoAnimoDTO e2 = crearEstado();
        FraseDTO creada = fraseService.guardar(new FraseRequest(Map.of("es", "Hola"), e1.id()));

        FraseDTO editada = fraseService.actualizar(creada.id(), new FraseRequest(Map.of("es", "Hola"), e2.id()));

        assertThat(editada.estadoAnimo().id()).isEqualTo(e2.id());
    }

    @Test
    void actualizarEstado_ModificaTraduccionesYCampos() {
        String codigo = codigoUnico();
        EstadoAnimoDTO creado = estadoService.guardar(estado(codigo, Map.of("es", "Feliz", "en", "Happy")));

        EstadoAnimoDTO editado = estadoService.actualizar(creado.id(), new EstadoAnimoRequest(
                codigo, "😄", null, null, null, "#000000", "#FFFFFF", "Inter", "bounce",
                Map.of("es", "Muy feliz")));

        assertThat(editado.traducciones()).containsExactlyEntriesOf(Map.of("es", "Muy feliz"));
        assertThat(editado.emoji()).isEqualTo("😄");
        assertThat(estadoService.obtenerPorId(creado.id()).traducciones())
                .containsExactlyEntriesOf(Map.of("es", "Muy feliz"));
    }

    @Test
    void actualizarEstado_CodigoInmutableSigueRechazado() {
        EstadoAnimoDTO creado = crearEstado();

        assertThatThrownBy(() -> estadoService.actualizar(creado.id(), estado(codigoUnico(), Map.of("es", "X"))))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("error.codigo.immutable");
    }

    // --- Hallazgo 2: borrar un estado con frases debe dar un error claro y no borrar nada ---

    @Test
    void eliminarEstadoConFrases_RechazaConMensajeClaro() {
        EstadoAnimoDTO e = crearEstado();
        fraseService.guardar(new FraseRequest(Map.of("es", "Uno"), e.id()));
        fraseService.guardar(new FraseRequest(Map.of("es", "Dos"), e.id()));

        assertThatThrownBy(() -> estadoService.eliminar(e.id()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("error.estadoanimo.hasfrases")
                .satisfies(ex -> assertThat(((BadRequestException) ex).getArgs()).containsExactly(2L));

        assertThat(estadoService.obtenerPorId(e.id())).isNotNull();
    }

    @Test
    void eliminarEstadoSinFrases_LoBorra() {
        EstadoAnimoDTO e = crearEstado();

        estadoService.eliminar(e.id());

        assertThatThrownBy(() -> estadoService.obtenerPorId(e.id()))
                .hasMessage("error.estadoanimo.notfound");
    }

    // --- Hallazgo 3: traducciones vacías no deben anular el fallback a "es" ---

    @Test
    void traduccionVaciaSeDescarta_YCaeAEspanol() {
        EstadoAnimoDTO e = crearEstado();
        Map<String, String> traducciones = new HashMap<>();
        traducciones.put("es", "Hola");
        traducciones.put("en", "   ");

        FraseDTO creada = fraseService.guardar(new FraseRequest(traducciones, e.id()));
        assertThat(creada.traducciones()).containsExactlyEntriesOf(Map.of("es", "Hola"));

        LocaleContextHolder.setLocale(Locale.ENGLISH);
        assertThat(fraseService.obtenerPorId(creada.id()).texto()).isEqualTo("Hola");
    }

    @Test
    void traduccionNulaSeDescarta() {
        EstadoAnimoDTO e = crearEstado();
        Map<String, String> traducciones = new HashMap<>();
        traducciones.put("es", "Hola");
        traducciones.put("en", null);

        FraseDTO creada = fraseService.guardar(new FraseRequest(traducciones, e.id()));

        assertThat(creada.traducciones()).containsExactlyEntriesOf(Map.of("es", "Hola"));
    }

    @Test
    void nombreDeEstadoVacioEnIngles_CaeAEspanol() {
        Map<String, String> traducciones = new HashMap<>();
        traducciones.put("es", "Feliz");
        traducciones.put("en", "");
        EstadoAnimoDTO creado = estadoService.guardar(estado(codigoUnico(), traducciones));

        LocaleContextHolder.setLocale(Locale.ENGLISH);
        assertThat(estadoService.obtenerPorId(creado.id()).nombre()).isEqualTo("Feliz");
    }

    @Test
    void espanolEnBlancoSigueSiendoObligatorio() {
        EstadoAnimoDTO e = crearEstado();

        assertThatThrownBy(() -> fraseService.guardar(new FraseRequest(Map.of("es", " ", "en", "Hi"), e.id())))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("error.traducciones.esrequired");
    }
}
