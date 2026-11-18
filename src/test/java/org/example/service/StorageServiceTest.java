package org.example.service;

import org.example.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import software.amazon.awssdk.services.s3.S3Client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

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

    private static final byte[] PNG = {
            (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'
    };

    private String keySubida(String nombreOriginal) {
        S3Client s3 = mock(S3Client.class);
        StorageService service = new StorageService(s3, "test-bucket", "tonin-api", "https://s3.us-west-1.wasabisys.com");
        String url = service.uploadFile(new MockMultipartFile("file", nombreOriginal, "image/png", PNG), "imagenes");
        String key = url.substring("https://s3.us-west-1.wasabisys.com/test-bucket/".length());
        assertTrue(service.esKeyPropia(key), "la key subida debe ser reconocida como propia: " + key);
        return key;
    }

    @Test
    void subirArchivo_ConservaUnaExtensionNormal() {
        assertTrue(keySubida("foto.PNG").endsWith(".PNG"));
    }

    @Test
    void subirArchivo_ConExtensionRara_GeneraUnaKeyQueLaAppReconoce() {
        // Antes la extensión venía tal cual del nombre del cliente y podía generar keys
        // que la app luego no reconocía como propias (nunca se limpiaban).
        assertFalse(keySubida("mi foto.p ng").contains(" "));
        assertFalse(keySubida("x./../otra-app").contains(".."));
        assertFalse(keySubida("sin-extension").contains("."));
        assertFalse(keySubida("foto.ñandú").contains("ñ"));
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
