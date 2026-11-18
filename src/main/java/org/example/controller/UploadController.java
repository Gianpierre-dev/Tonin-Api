package org.example.controller;

import org.example.dto.UploadResponse;
import org.example.service.ArchivoService;
import org.example.service.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final StorageService storageService;
    private final ArchivoService archivoService;

    public UploadController(StorageService storageService, ArchivoService archivoService) {
        this.storageService = storageService;
        this.archivoService = archivoService;
    }

    @PostMapping(value = "/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> subirImagen(@RequestParam("file") MultipartFile file) {
        String url = storageService.uploadFile(file, "imagenes");
        return ResponseEntity.status(HttpStatus.CREATED).body(new UploadResponse(url, "imagen"));
    }

    @PostMapping(value = "/musica", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> subirMusica(@RequestParam("file") MultipartFile file) {
        String url = storageService.uploadFile(file, "musica");
        return ResponseEntity.status(HttpStatus.CREATED).body(new UploadResponse(url, "musica"));
    }

    @DeleteMapping
    public ResponseEntity<Void> eliminarArchivo(@RequestParam String url) {
        archivoService.eliminarArchivoSuelto(url);
        return ResponseEntity.noContent().build();
    }
}
