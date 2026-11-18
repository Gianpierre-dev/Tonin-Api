package org.example.service;

import org.example.repository.EstadoAnimoRepository;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArchivoServiceTest {

    @Test
    void limpieza_SeAbortaSiNingunEstadoUsaArchivos() {
        // Base vacía, restaurada vacía o de otro entorno con la misma carpeta: todo parecería
        // huérfano y se borraría el bucket entero.
        StorageService storage = mock(StorageService.class);
        EstadoAnimoRepository repo = mock(EstadoAnimoRepository.class);
        when(repo.findArchivoUrlsEnUso()).thenReturn(Set.of());
        when(storage.listarArchivos()).thenReturn(List.of(S3Object.builder()
                .key("tonin-api/imagenes/a.png")
                .lastModified(Instant.now().minus(48, ChronoUnit.HOURS))
                .build()));
        when(storage.esKeyPropia(anyString())).thenReturn(true);
        ArchivoService service = new ArchivoService(storage, repo, "tonin-api", "borrar", 24);

        assertThat(service.limpiarHuerfanos(true)).isEmpty();
        verify(storage, never()).deleteKey(anyString());
    }
}
