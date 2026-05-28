package org.example.service;

import org.example.dto.EstadoAnimoDTO;
import org.example.dto.EstadoAnimoRequest;
import org.example.exception.ResourceNotFoundException;
import org.example.model.EstadoAnimo;
import org.example.repository.EstadoAnimoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EstadoAnimoService implements IEstadoAnimoService {

    private final EstadoAnimoRepository repository;

    public EstadoAnimoService(EstadoAnimoRepository repository) {
        this.repository = repository;
    }

    @Override
    public EstadoAnimoDTO guardar(EstadoAnimoRequest request) {
        EstadoAnimo estado = new EstadoAnimo(
                request.nombre(), request.emoji(), request.iconUrl(), request.musicaUrl(), request.imagenUrl(),
                request.colorPrimario(), request.colorSecundario(), request.fontFamily(), request.animationType()
        );
        return EstadoAnimoDTO.fromEntity(repository.save(estado));
    }

    @Override
    public List<EstadoAnimoDTO> obtenerTodos() {
        return repository.findAll().stream()
                .map(EstadoAnimoDTO::fromEntity)
                .toList();
    }

    @Override
    public EstadoAnimoDTO obtenerPorId(Long id) {
        return repository.findById(id)
                .map(EstadoAnimoDTO::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("error.estadoanimo.notfound", id));
    }

    @Override
    public EstadoAnimoDTO actualizar(Long id, EstadoAnimoRequest request) {
        EstadoAnimo estado = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("error.estadoanimo.notfound", id));

        estado.setNombre(request.nombre());
        estado.setEmoji(request.emoji());
        estado.setIconUrl(request.iconUrl());
        estado.setMusicaUrl(request.musicaUrl());
        estado.setImagenUrl(request.imagenUrl());
        estado.setColorPrimario(request.colorPrimario());
        estado.setColorSecundario(request.colorSecundario());
        estado.setFontFamily(request.fontFamily());
        estado.setAnimationType(request.animationType());
        return EstadoAnimoDTO.fromEntity(repository.save(estado));
    }

    @Override
    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("error.estadoanimo.notfound", id);
        }
        repository.deleteById(id);
    }
}
