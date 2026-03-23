package org.example.service;

import org.example.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

class StorageServiceTest {

    private StorageService storageService;

    @BeforeEach
    void setUp() {
        // Creamos el servicio sin S3Client real - solo validamos la lógica de validación
        storageService = new StorageService(
                null, "test-bucket", "tonin-api", "https://s3.us-west-1.wasabisys.com"
        );
    }

    @Test
    void subirImagenConTipoInvalido_DebeLanzarExcepcion() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "virus.exe", "application/octet-stream", "contenido".getBytes()
        );

        assertThrows(BadRequestException.class, () ->
                storageService.uploadFile(file, "imagenes")
        );
    }

    @Test
    void subirAudioConTipoInvalido_DebeLanzarExcepcion() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "foto.png", "image/png", "contenido".getBytes()
        );

        assertThrows(BadRequestException.class, () ->
                storageService.uploadFile(file, "musica")
        );
    }

    @Test
    void subirArchivoVacio_DebeLanzarExcepcion() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "vacio.png", "image/png", new byte[0]
        );

        assertThrows(BadRequestException.class, () ->
                storageService.uploadFile(file, "imagenes")
        );
    }

    @Test
    void subfolderInvalido_DebeLanzarExcepcion() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "archivo.txt", "text/plain", "contenido".getBytes()
        );

        assertThrows(BadRequestException.class, () ->
                storageService.uploadFile(file, "documentos")
        );
    }

    @Test
    void eliminarArchivoConUrlNula_NoDebeLanzarExcepcion() {
        assertDoesNotThrow(() -> storageService.deleteFile(null));
        assertDoesNotThrow(() -> storageService.deleteFile(""));
    }

    @Test
    void eliminarArchivoConUrlExterna_NoDebeLanzarExcepcion() {
        // URL que no pertenece a nuestro bucket - debe ser ignorada
        assertDoesNotThrow(() ->
                storageService.deleteFile("https://otro-dominio.com/archivo.png")
        );
    }
}
