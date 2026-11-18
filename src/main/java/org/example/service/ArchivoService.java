package org.example.service;

import org.example.exception.BadRequestException;
import org.example.repository.EstadoAnimoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ciclo de vida de los archivos subidos a Wasabi: evita que queden archivos huérfanos
 * y que se borren archivos ajenos a la app o todavía en uso.
 *
 * <p>Un archivo está "en uso" si algún EstadoAnimo lo referencia en iconUrl, musicaUrl o imagenUrl.</p>
 */
@Service
public class ArchivoService {

    private static final Logger log = LoggerFactory.getLogger(ArchivoService.class);

    private final StorageService storageService;
    private final EstadoAnimoRepository estadoRepository;
    private final String folder;
    private final boolean limpiezaBorra;
    private final Duration antiguedadMinima;

    public ArchivoService(StorageService storageService,
                          EstadoAnimoRepository estadoRepository,
                          @Value("${wasabi.folder}") String folder,
                          @Value("${wasabi.limpieza.modo:simular}") String modoLimpieza,
                          @Value("${wasabi.limpieza.antiguedad-horas:24}") long antiguedadHoras) {
        this.storageService = storageService;
        this.estadoRepository = estadoRepository;
        this.folder = folder;
        this.limpiezaBorra = "borrar".equalsIgnoreCase(modoLimpieza);
        this.antiguedadMinima = Duration.ofHours(antiguedadHoras);
    }

    /**
     * DELETE /api/uploads: borra un archivo suelto (por ejemplo, subido y descartado en el formulario).
     * Solo acepta archivos propios de la app y que ningún estado esté usando.
     */
    public void eliminarArchivoSuelto(String url) {
        String key = storageService.keyDeUrlPropia(url)
                .orElseThrow(() -> new BadRequestException("error.file.url.invalid"));
        if (estadoRepository.existsByArchivoUrl(url)) {
            throw new BadRequestException("error.file.inuse");
        }
        storageService.deleteKey(key);
    }

    /**
     * Programa el borrado de las URLs que dejaron de usarse (por una edición o un borrado de estado).
     * Debe llamarse dentro de la transacción y después de hacer flush de los cambios, para que la
     * consulta de uso vea el estado nuevo. El borrado real ocurre después del commit: si la
     * transacción hace rollback, los archivos se conservan. Un fallo de Wasabi solo se registra en
     * el log; la limpieza periódica lo reintentará.
     */
    public void programarBorradoSiHuerfanos(Collection<String> urlsLiberadas) {
        List<String> keys = urlsLiberadas.stream()
                .filter(Objects::nonNull)
                .distinct()
                .filter(url -> !estadoRepository.existsByArchivoUrl(url))
                .map(storageService::keyDeUrlPropia)
                .flatMap(Optional::stream)
                .toList();
        if (keys.isEmpty()) return;

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    borrarSinFallar(keys);
                }
            });
        } else {
            borrarSinFallar(keys);
        }
    }

    @Scheduled(cron = "${wasabi.limpieza.cron:0 0 4 * * *}")
    public void limpiezaProgramada() {
        try {
            limpiarHuerfanos(limpiezaBorra);
        } catch (Exception e) {
            log.error("Falló la limpieza de archivos huérfanos en Wasabi", e);
        }
    }

    /**
     * Busca archivos propios en {folder}/imagenes y {folder}/musica que ningún estado usa y que
     * tienen más de la antigüedad mínima (para no borrar un archivo recién subido cuyo formulario
     * todavía no se guardó).
     *
     * @param borrar si es false, solo los reporta en el log (modo simulación).
     * @return las keys huérfanas encontradas.
     */
    public List<String> limpiarHuerfanos(boolean borrar) {
        Set<String> urlsEnUso = estadoRepository.findArchivoUrlsEnUso();

        // Salvaguarda: sin ninguna URL en uso (base vacía, restaurada vacía u otro entorno que
        // comparte la carpeta), todo el bucket parecería huérfano. Es más seguro no borrar nada.
        if (urlsEnUso.isEmpty()) {
            log.warn("Limpieza abortada: ningún estado de ánimo referencia archivos");
            return List.of();
        }

        // Salvaguarda: si hay URLs en uso que parecen de la app pero no coinciden con el formato
        // actual (por ejemplo, cambió el endpoint), no se puede saber qué está en uso. Se aborta.
        List<String> irreconocibles = urlsEnUso.stream()
                .filter(url -> url.contains("/" + folder + "/"))
                .filter(url -> storageService.keyDeUrlPropia(url).isEmpty())
                .toList();
        if (!irreconocibles.isEmpty()) {
            log.error("Limpieza abortada: {} URL(s) en uso con formato no reconocido, ej. {}",
                    irreconocibles.size(), irreconocibles.get(0));
            return List.of();
        }

        Set<String> keysEnUso = urlsEnUso.stream()
                .map(storageService::keyDeUrlPropia)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());
        Instant limite = Instant.now().minus(antiguedadMinima);

        List<String> huerfanas = storageService.listarArchivos().stream()
                .filter(o -> storageService.esKeyPropia(o.key()))
                .filter(o -> !keysEnUso.contains(o.key()))
                .filter(o -> o.lastModified() != null && o.lastModified().isBefore(limite))
                .map(S3Object::key)
                .toList();

        if (huerfanas.isEmpty()) return huerfanas;
        if (borrar) {
            log.info("Limpieza de Wasabi: borrando {} archivo(s) huérfano(s)", huerfanas.size());
            borrarSinFallar(huerfanas);
        } else {
            log.info("Limpieza de Wasabi (simulación): {} archivo(s) huérfano(s) que se borrarían: {}",
                    huerfanas.size(), huerfanas);
        }
        return huerfanas;
    }

    private void borrarSinFallar(List<String> keys) {
        for (String key : keys) {
            try {
                storageService.deleteKey(key);
            } catch (Exception e) {
                log.warn("No se pudo borrar el archivo {} de Wasabi: {}", key, e.getMessage());
            }
        }
    }
}
