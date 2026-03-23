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
                request.nombre(), request.emoji(), request.musicaUrl(), request.imagenUrl(),
                request.colorPrimario(), request.colorSecundario(), request.fontFamily(), request.animationType()
        );
        return convertToDTO(repository.save(estado));
    }

    @Override
    public List<EstadoAnimoDTO> obtenerTodos() {
        return repository.findAll().stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    public EstadoAnimoDTO obtenerPorId(Long id) {
        return repository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Estado de ánimo", id));
    }

    @Override
    public EstadoAnimoDTO actualizar(Long id, EstadoAnimoRequest request) {
        EstadoAnimo estado = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estado de ánimo", id));

        estado.setNombre(request.nombre());
        estado.setEmoji(request.emoji());
        estado.setMusicaUrl(request.musicaUrl());
        estado.setImagenUrl(request.imagenUrl());
        estado.setColorPrimario(request.colorPrimario());
        estado.setColorSecundario(request.colorSecundario());
        estado.setFontFamily(request.fontFamily());
        estado.setAnimationType(request.animationType());
        return convertToDTO(repository.save(estado));
    }

    @Override
    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Estado de ánimo", id);
        }
        repository.deleteById(id);
    }

    private EstadoAnimoDTO convertToDTO(EstadoAnimo estado) {
        return new EstadoAnimoDTO(
                estado.getId(),
                estado.getNombre(),
                estado.getEmoji(),
                estado.getMusicaUrl(),
                estado.getImagenUrl(),
                estado.getColorPrimario(),
                estado.getColorSecundario(),
                estado.getFontFamily(),
                estado.getAnimationType()
        );
    }
}
