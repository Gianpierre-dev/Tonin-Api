package org.example.service;

import org.apache.tika.Tika;
import org.example.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class StorageService {

    private static final List<String> SUBCARPETAS = List.of("imagenes", "musica");

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private static final Set<String> ALLOWED_AUDIO_TYPES = Set.of(
            "audio/mpeg", "audio/wav", "audio/ogg", "audio/mp4", "audio/webm"
    );

    private final S3Client s3Client;
    private final String bucketName;
    private final String folder;
    private final String endpoint;
    private final Pattern keyPropiaPattern;
    private final Tika tika = new Tika();

    public StorageService(S3Client s3Client,
                          @Value("${wasabi.bucket-name}") String bucketName,
                          @Value("${wasabi.folder}") String folder,
                          @Value("${wasabi.endpoint}") String endpoint) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.folder = folder;
        this.endpoint = endpoint;
        this.keyPropiaPattern = Pattern.compile(
                Pattern.quote(folder) + "/(" + String.join("|", SUBCARPETAS) + ")/[A-Za-z0-9][A-Za-z0-9._-]*");
    }

    public String uploadFile(MultipartFile file, String subfolder) {
        validateFile(file, subfolder);

        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        String key = folder + "/" + subfolder + "/" + UUID.randomUUID() + extension;

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .acl("public-read")
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));

            return endpoint + "/" + bucketName + "/" + key;
        } catch (IOException e) {
            throw new BadRequestException("error.upload.failed", e.getMessage());
        }
    }

    /**
     * Borra el archivo si la URL apunta a un archivo propio de la app (ver {@link #keyDeUrlPropia}).
     * Cualquier otra URL se ignora: el bucket puede estar compartido con otros proyectos.
     */
    public void deleteFile(String fileUrl) {
        keyDeUrlPropia(fileUrl).ifPresent(this::deleteKey);
    }

    /** Borra un objeto por su key, solo si está dentro de las subcarpetas de la app. */
    public void deleteKey(String key) {
        if (!esKeyPropia(key)) {
            throw new IllegalArgumentException("Key fuera de la carpeta de la app: " + key);
        }
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build());
    }

    /**
     * Devuelve la key si la URL es exactamente la de un archivo subido por la app:
     * {endpoint}/{bucket}/{folder}/(imagenes|musica)/{nombre}, sin subcarpetas ni "..".
     */
    public Optional<String> keyDeUrlPropia(String fileUrl) {
        if (fileUrl == null) return Optional.empty();
        String prefix = endpoint + "/" + bucketName + "/";
        if (!fileUrl.startsWith(prefix)) return Optional.empty();
        String key = fileUrl.substring(prefix.length());
        return esKeyPropia(key) ? Optional.of(key) : Optional.empty();
    }

    /** True si la key corresponde a un archivo de {folder}/imagenes/ o {folder}/musica/. */
    public boolean esKeyPropia(String key) {
        return key != null && keyPropiaPattern.matcher(key).matches();
    }

    /**
     * Lista todos los objetos bajo {folder}/, recorriendo las páginas del listado.
     */
    public List<S3Object> listarArchivos() {
        List<S3Object> objetos = new ArrayList<>();
        String continuationToken = null;
        do {
            ListObjectsV2Response respuesta = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(folder + "/")
                    .continuationToken(continuationToken)
                    .build());
            objetos.addAll(respuesta.contents());
            continuationToken = Boolean.TRUE.equals(respuesta.isTruncated())
                    ? respuesta.nextContinuationToken()
                    : null;
        } while (continuationToken != null);
        return objetos;
    }

    private void validateFile(MultipartFile file, String subfolder) {
        if (file.isEmpty()) {
            throw new BadRequestException("error.file.empty");
        }

        Set<String> permitidos;
        switch (subfolder) {
            case "imagenes" -> permitidos = ALLOWED_IMAGE_TYPES;
            case "musica" -> permitidos = ALLOWED_AUDIO_TYPES;
            default -> throw new BadRequestException("error.subfolder.invalid", subfolder);
        }

        // 1) Whitelist sobre el Content-Type declarado por el cliente (puede ser falseado).
        String contentType = file.getContentType();
        if (!permitidos.contains(contentType)) {
            if (permitidos == ALLOWED_IMAGE_TYPES) {
                throw new BadRequestException("error.file.imagetype");
            } else {
                throw new BadRequestException("error.file.audiotype");
            }
        }

        // 2) Validacion de magic bytes con Tika: detecta el tipo REAL del contenido.
        // Si alguien manda un .exe con Content-Type: image/jpeg, aca se rechaza.
        try {
            String tipoReal = tika.detect(file.getInputStream());
            if (!permitidos.contains(tipoReal)) {
                throw new BadRequestException("error.file.invalidcontent");
            }
        } catch (IOException e) {
            throw new BadRequestException("error.file.invalidcontent");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf("."));
    }
}
