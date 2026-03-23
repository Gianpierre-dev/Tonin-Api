package org.example.controller;

import jakarta.validation.Valid;
import org.example.dto.EstadoAnimoDTO;
import org.example.dto.EstadoAnimoRequest;
import org.example.service.IEstadoAnimoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estados")
public class EstadoAnimoController {

    private final IEstadoAnimoService service;

    public EstadoAnimoController(IEstadoAnimoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<EstadoAnimoDTO>> listarEstados() {
        return ResponseEntity.ok(service.obtenerTodos());
    }

    @PostMapping
    public ResponseEntity<EstadoAnimoDTO> crearEstado(@Valid @RequestBody EstadoAnimoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.guardar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EstadoAnimoDTO> actualizarEstado(@PathVariable Long id,
                                                           @Valid @RequestBody EstadoAnimoRequest request) {
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarEstado(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
