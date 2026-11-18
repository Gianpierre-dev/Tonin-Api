package org.example.service;

import org.example.dto.EstadoAnimoDTO;
import org.example.dto.EstadoAnimoRequest;
import org.example.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Ciclo de vida de los archivos en Wasabi (S3 simulado con Mockito, BD H2).
 * Perfil test: endpoint https://s3.us-west-1.wasabisys.com, bucket test-bucket, carpeta test.
 */
@SpringBootTest
@ActiveProfiles("test")
class ArchivosWasabiIntegrationTest {

    private static final String PREFIJO = "https://s3.us-west-1.wasabisys.com/test-bucket/";

    @MockitoBean
    private S3Client s3Client;

    @Autowired
    private IEstadoAnimoService estadoService;

    @Autowired
    private ArchivoService archivoService;

    @BeforeEach
    void limpiarMock() {
        clearInvocations(s3Client);
    }

    private static String urlImagen() {
        return PREFIJO + "test/imagenes/" + UUID.randomUUID() + ".png";
    }

    private static String urlMusica() {
        return PREFIJO + "test/musica/" + UUID.randomUUID() + ".mp3";
    }

    private static String keyDe(String url) {
        return url.substring(PREFIJO.length());
    }

    private static String codigoUnico() {
        return "w-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static EstadoAnimoRequest estado(String codigo, String icon, String musica, String imagen) {
        return new EstadoAnimoRequest(codigo, "😊", icon, musica, imagen, "#FFD700", "#FFA500",
                "Poppins", "float", Map.of("es", "Feliz"));
    }

    private List<String> keysBorradas() {
        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client, org.mockito.Mockito.atLeast(0)).deleteObject(captor.capture());
        captor.getAllValues().forEach(r -> assertThat(r.bucket()).isEqualTo("test-bucket"));
        return captor.getAllValues().stream().map(DeleteObjectRequest::key).toList();
    }

    // --- Archivos huérfanos al actualizar o eliminar un estado ---

    @Test
    void actualizarEstado_BorraLosArchivosReemplazados() {
        String codigo = codigoUnico();
        String imagenVieja = urlImagen();
        String musicaVieja = urlMusica();
        String icono = urlImagen();
        EstadoAnimoDTO creado = estadoService.guardar(estado(codigo, icono, musicaVieja, imagenVieja));

        String imagenNueva = urlImagen();
        estadoService.actualizar(creado.id(), estado(codigo, icono, null, imagenNueva));

        assertThat(keysBorradas()).containsExactlyInAnyOrder(keyDe(imagenVieja), keyDe(musicaVieja));
    }

    @Test
    void actualizarEstado_SinCambiosDeUrl_NoBorraNada() {
        String codigo = codigoUnico();
        String imagen = urlImagen();
        EstadoAnimoDTO creado = estadoService.guardar(estado(codigo, null, null, imagen));

        estadoService.actualizar(creado.id(), estado(codigo, null, null, imagen));

        assertThat(keysBorradas()).isEmpty();
    }

    @Test
    void actualizarEstado_NoBorraUnArchivoQueOtroEstadoSigueUsando() {
        String compartida = urlImagen();
        estadoService.guardar(estado(codigoUnico(), null, null, compartida));
        String codigo = codigoUnico();
        EstadoAnimoDTO otro = estadoService.guardar(estado(codigo, null, null, compartida));

        estadoService.actualizar(otro.id(), estado(codigo, null, null, urlImagen()));

        assertThat(keysBorradas()).isEmpty();
    }

    @Test
    void actualizarEstado_NoTocaUrlsAjenasAlBucket() {
        String codigo = codigoUnico();
        EstadoAnimoDTO creado = estadoService.guardar(
                estado(codigo, null, null, "https://cdn.externo.com/foto.png"));

        estadoService.actualizar(creado.id(), estado(codigo, null, null, urlImagen()));

        assertThat(keysBorradas()).isEmpty();
    }

    @Test
    void eliminarEstado_BorraSusArchivos() {
        String icono = urlImagen();
        String musica = urlMusica();
        String imagen = urlImagen();
        EstadoAnimoDTO creado = estadoService.guardar(estado(codigoUnico(), icono, musica, imagen));

        estadoService.eliminar(creado.id());

        assertThat(keysBorradas()).containsExactlyInAnyOrder(keyDe(icono), keyDe(musica), keyDe(imagen));
    }

    @Test
    void eliminarEstado_SiWasabiFalla_ElEstadoIgualSeElimina() {
        String imagen = urlImagen();
        EstadoAnimoDTO creado = estadoService.guardar(estado(codigoUnico(), null, null, imagen));
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenThrow(new RuntimeException("Wasabi caído"));

        estadoService.eliminar(creado.id());

        assertThatThrownBy(() -> estadoService.obtenerPorId(creado.id()))
                .hasMessage("error.estadoanimo.notfound");
    }

    // --- DELETE /api/uploads?url= ---

    @Test
    void eliminarArchivoSuelto_PropioYSinUso_LoBorra() {
        String url = urlImagen();

        archivoService.eliminarArchivoSuelto(url);

        assertThat(keysBorradas()).containsExactly(keyDe(url));
    }

    @Test
    void eliminarArchivoSuelto_EnUsoPorUnEstado_LoRechaza() {
        String url = urlMusica();
        estadoService.guardar(estado(codigoUnico(), null, url, null));

        assertThatThrownBy(() -> archivoService.eliminarArchivoSuelto(url))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("error.file.inuse");
        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void eliminarArchivoSuelto_FueraDeLaCarpetaDeLaApp_LoRechaza() {
        List<String> ajenas = List.of(
                PREFIJO + "otra-app/backup.zip",
                PREFIJO + "test/documentos/x.pdf",
                PREFIJO + "test/imagenes/../../otra-app/x.png",
                PREFIJO + "test/imagenes/sub/x.png",
                "https://cdn.externo.com/test/imagenes/x.png",
                "");

        for (String url : ajenas) {
            assertThatThrownBy(() -> archivoService.eliminarArchivoSuelto(url))
                    .as(url)
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("error.file.url.invalid");
        }
        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    // --- Limpieza periódica de lo subido y nunca guardado ---

    private void bucketConObjetos(S3Object... objetos) {
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(ListObjectsV2Response.builder().contents(objetos).isTruncated(false).build());
    }

    private static S3Object objeto(String key, Instant modificado) {
        return S3Object.builder().key(key).lastModified(modificado).build();
    }

    @Test
    void limpieza_BorraSoloHuerfanosAntiguosDeLaCarpeta() {
        Instant viejo = Instant.now().minus(48, ChronoUnit.HOURS);
        String enUso = urlImagen();
        estadoService.guardar(estado(codigoUnico(), null, null, enUso));
        String huerfano = "test/imagenes/" + UUID.randomUUID() + ".png";
        String reciente = "test/musica/" + UUID.randomUUID() + ".mp3";
        bucketConObjetos(
                objeto(keyDe(enUso), viejo),
                objeto(huerfano, viejo),
                objeto(reciente, Instant.now().minus(1, ChronoUnit.HOURS)),
                objeto("test/otra-cosa/x.bin", viejo));

        List<String> huerfanos = archivoService.limpiarHuerfanos(true);

        assertThat(huerfanos).containsExactly(huerfano);
        assertThat(keysBorradas()).containsExactly(huerfano);
        ArgumentCaptor<ListObjectsV2Request> listado = ArgumentCaptor.forClass(ListObjectsV2Request.class);
        verify(s3Client, times(1)).listObjectsV2(listado.capture());
        assertThat(listado.getValue().prefix()).isEqualTo("test/");
    }

    @Test
    void limpieza_SeAbortaSiHayUrlsEnUsoConFormatoDesconocido() {
        // Por ejemplo, si cambió el endpoint: no se puede saber qué archivos siguen en uso.
        EstadoAnimoDTO raro = estadoService.guardar(estado(codigoUnico(), null, null,
                "https://otro-endpoint.com/test-bucket/test/imagenes/" + UUID.randomUUID() + ".png"));
        try {
            bucketConObjetos(objeto("test/imagenes/" + UUID.randomUUID() + ".png",
                    Instant.now().minus(48, ChronoUnit.HOURS)));

            List<String> huerfanos = archivoService.limpiarHuerfanos(true);

            assertThat(huerfanos).isEmpty();
            verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
        } finally {
            // La BD H2 se comparte entre tests: este estado haría abortar las otras limpiezas.
            estadoService.eliminar(raro.id());
        }
    }

    @Test
    void limpieza_RecorreTodasLasPaginasDelListado() {
        estadoService.guardar(estado(codigoUnico(), null, null, urlImagen()));
        Instant viejo = Instant.now().minus(48, ChronoUnit.HOURS);
        String enPagina1 = "test/imagenes/" + UUID.randomUUID() + ".png";
        String enPagina2 = "test/musica/" + UUID.randomUUID() + ".mp3";
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(ListObjectsV2Response.builder().contents(objeto(enPagina1, viejo))
                        .isTruncated(true).nextContinuationToken("pagina-2").build())
                .thenReturn(ListObjectsV2Response.builder().contents(objeto(enPagina2, viejo))
                        .isTruncated(false).build());

        List<String> huerfanos = archivoService.limpiarHuerfanos(false);

        assertThat(huerfanos).contains(enPagina1, enPagina2);
        ArgumentCaptor<ListObjectsV2Request> listado = ArgumentCaptor.forClass(ListObjectsV2Request.class);
        verify(s3Client, times(2)).listObjectsV2(listado.capture());
        assertThat(listado.getAllValues().get(1).continuationToken()).isEqualTo("pagina-2");
    }

    @Test
    void limpieza_EnModoSimulacion_NoBorraNada() {
        estadoService.guardar(estado(codigoUnico(), null, null, urlImagen()));
        String huerfano = "test/imagenes/" + UUID.randomUUID() + ".png";
        bucketConObjetos(objeto(huerfano, Instant.now().minus(48, ChronoUnit.HOURS)));

        List<String> huerfanos = archivoService.limpiarHuerfanos(false);

        assertThat(huerfanos).contains(huerfano);
        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }
}
