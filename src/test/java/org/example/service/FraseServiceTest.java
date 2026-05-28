package org.example.service;

import org.example.dto.FraseDTO;
import org.example.model.EstadoAnimo;
import org.example.model.Frase;
import org.example.repository.EstadoAnimoRepository;
import org.example.repository.FraseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraseServiceTest {

    @Mock
    private FraseRepository fraseRepository;

    @Mock
    private EstadoAnimoRepository estadoAnimoRepository;

    @InjectMocks
    private FraseService fraseService;

    private EstadoAnimo feliz;
    private Frase frase1;
    private Frase frase2;

    @BeforeEach
    void setUp() {
        feliz = new EstadoAnimo("feliz", "😊", null, null, null, "#FFD700", "#FFA500", "Poppins", "float");
        feliz.addTraduccion("es", "Feliz");
        feliz.addTraduccion("en", "Happy");
        feliz.setId(1L);

        frase1 = new Frase(feliz);
        frase1.addTraduccion("es", "Frase 1");
        frase1.setId(101L);

        frase2 = new Frase(feliz);
        frase2.addTraduccion("es", "Frase 2");
        frase2.setId(102L);
    }

    @Test
    void cuandoPidoFraseAleatoria_DebeRetornarUnaFraseValida() {
        when(estadoAnimoRepository.findByCodigoIgnoreCase("FELIZ")).thenReturn(Optional.of(feliz));
        when(fraseRepository.findRandomByEstadoAnimo(feliz)).thenReturn(Optional.of(frase1));

        Optional<FraseDTO> resultado = fraseService.obtenerFraseAleatoria("FELIZ", new ArrayList<>());

        assertTrue(resultado.isPresent());
        assertEquals("Feliz", resultado.get().estadoAnimo().nombre());
        verify(fraseRepository, times(1)).findRandomByEstadoAnimo(feliz);
    }

    @Test
    void cuandoExcluyoIds_DebeUsarQueryConExcluidos() {
        List<Long> excluidos = List.of(101L);

        when(estadoAnimoRepository.findByCodigoIgnoreCase("FELIZ")).thenReturn(Optional.of(feliz));
        when(fraseRepository.findRandomByEstadoAnimoAndIdNotIn(feliz, excluidos)).thenReturn(Optional.of(frase2));

        Optional<FraseDTO> resultado = fraseService.obtenerFraseAleatoria("FELIZ", excluidos);

        assertTrue(resultado.isPresent());
        assertEquals(102L, resultado.get().id());
    }

    @Test
    void cuandoNoHayFrasesDisponibles_DebeRetornarVacio() {
        List<Long> excluidos = List.of(101L, 102L);

        when(estadoAnimoRepository.findByCodigoIgnoreCase("FELIZ")).thenReturn(Optional.of(feliz));
        when(fraseRepository.findRandomByEstadoAnimoAndIdNotIn(feliz, excluidos)).thenReturn(Optional.empty());

        Optional<FraseDTO> resultado = fraseService.obtenerFraseAleatoria("FELIZ", excluidos);

        assertFalse(resultado.isPresent());
    }
}
