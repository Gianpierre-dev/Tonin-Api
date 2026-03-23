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
    private List<Frase> frasesFelices;

    @BeforeEach
    void setUp() {
        feliz = new EstadoAnimo("FELIZ", "😊", null, null, "#FFD700", "#FFA500", "Poppins", "float");
        feliz.setId(1L);

        frasesFelices = new ArrayList<>();
        Frase f1 = new Frase("Frase 1", feliz); f1.setId(101L);
        Frase f2 = new Frase("Frase 2", feliz); f2.setId(102L);
        frasesFelices.add(f1);
        frasesFelices.add(f2);
    }

    @Test
    void cuandoPidoFraseAleatoria_DebeRetornarUnaFraseValida() {
        when(estadoAnimoRepository.findByNombre("FELIZ")).thenReturn(Optional.of(feliz));
        when(fraseRepository.findByEstadoAnimo(feliz)).thenReturn(frasesFelices);

        Optional<FraseDTO> resultado = fraseService.obtenerFraseAleatoria("FELIZ", new ArrayList<>());

        assertTrue(resultado.isPresent());
        assertEquals("FELIZ", resultado.get().estadoAnimo().nombre());
        verify(fraseRepository, times(1)).findByEstadoAnimo(feliz);
    }

    @Test
    void cuandoExcluyoIds_DebeRetornarSoloLasNoExcluidas() {
        List<Long> excluidos = List.of(101L);
        Frase f2 = frasesFelices.get(1);

        when(estadoAnimoRepository.findByNombre("FELIZ")).thenReturn(Optional.of(feliz));
        when(fraseRepository.findByEstadoAnimoAndIdNotIn(feliz, excluidos)).thenReturn(List.of(f2));

        Optional<FraseDTO> resultado = fraseService.obtenerFraseAleatoria("FELIZ", excluidos);

        assertTrue(resultado.isPresent());
        assertEquals(102L, resultado.get().id());
    }

    @Test
    void cuandoTodasEstanExcluidas_DebeRetornarVacio() {
        List<Long> excluidos = List.of(101L, 102L);

        when(estadoAnimoRepository.findByNombre("FELIZ")).thenReturn(Optional.of(feliz));
        when(fraseRepository.findByEstadoAnimoAndIdNotIn(feliz, excluidos)).thenReturn(List.of());

        Optional<FraseDTO> resultado = fraseService.obtenerFraseAleatoria("FELIZ", excluidos);

        assertFalse(resultado.isPresent());
    }
}
