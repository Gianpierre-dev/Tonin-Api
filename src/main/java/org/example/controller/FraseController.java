package org.example.controller;

import jakarta.validation.Valid;
import org.example.dto.FraseDTO;
import org.example.dto.FraseRequest;
import org.example.service.IFraseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/frases")
public class FraseController {

    private final IFraseService service;

    public FraseController(IFraseService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<FraseDTO> crearFrase(@Valid @RequestBody FraseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.guardar(request));
    }

    @GetMapping
    public ResponseEntity<List<FraseDTO>> listarFrases() {
        return ResponseEntity.ok(service.obtenerTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FraseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FraseDTO> actualizarFrase(@PathVariable Long id, @Valid @RequestBody FraseRequest request) {
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarFrase(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/random")
    public ResponseEntity<FraseDTO> obtenerFraseAleatoria(
            @RequestParam String animo,
            @RequestParam(required = false, defaultValue = "") List<Long> excluidos) {
        return service.obtenerFraseAleatoria(animo, excluidos)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
