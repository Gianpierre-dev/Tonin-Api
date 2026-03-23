package org.example.service;

import org.example.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Service
public class StorageService {

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

    public StorageService(S3Client s3Client,
                          @Value("${wasabi.bucket-name}") String bucketName,
                          @Value("${wasabi.folder}") String folder,
                          @Value("${wasabi.endpoint}") String endpoint) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.folder = folder;
        this.endpoint = endpoint;
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
            throw new BadRequestException("Error al subir el archivo: " + e.getMessage());
        }
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return;

        String prefix = endpoint + "/" + bucketName + "/";
        if (!fileUrl.startsWith(prefix)) return;

        String key = fileUrl.substring(prefix.length());

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.deleteObject(request);
    }

    private void validateFile(MultipartFile file, String subfolder) {
        if (file.isEmpty()) {
            throw new BadRequestException("El archivo está vacío");
        }

        String contentType = file.getContentType();

        switch (subfolder) {
            case "imagenes" -> {
                if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
                    throw new BadRequestException("Tipo de imagen no permitido. Usa: JPEG, PNG, GIF o WebP");
                }
            }
            case "musica" -> {
                if (!ALLOWED_AUDIO_TYPES.contains(contentType)) {
                    throw new BadRequestException("Tipo de audio no permitido. Usa: MP3, WAV, OGG, MP4 o WebM");
                }
            }
            default -> throw new BadRequestException("Subfolder no válido: " + subfolder);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf("."));
    }
}
