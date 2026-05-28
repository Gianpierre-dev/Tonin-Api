package org.example.service;

import org.example.dto.EstadoAnimoDTO;
import org.example.dto.EstadoAnimoRequest;
import org.example.exception.ResourceNotFoundException;
import org.example.model.EstadoAnimo;
import org.example.repository.EstadoAnimoRepository;
import org.springframework.context.i18n.LocaleContextHolder;
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
        String locale = LocaleContextHolder.getLocale().getLanguage();
        EstadoAnimo estado = new EstadoAnimo(
                request.codigo(), request.emoji(), request.iconUrl(), request.musicaUrl(), request.imagenUrl(),
                request.colorPrimario(), request.colorSecundario(), request.fontFamily(), request.animationType()
        );
        request.traducciones().forEach(estado::addTraduccion);
        return EstadoAnimoDTO.fromEntity(repository.save(estado), locale);
    }

    @Override
    public List<EstadoAnimoDTO> obtenerTodos() {
        String locale = LocaleContextHolder.getLocale().getLanguage();
        return repository.findAll().stream()
                .map(e -> EstadoAnimoDTO.fromEntity(e, locale))
                .toList();
    }

    @Override
    public EstadoAnimoDTO obtenerPorId(Long id) {
        String locale = LocaleContextHolder.getLocale().getLanguage();
        return repository.findById(id)
                .map(e -> EstadoAnimoDTO.fromEntity(e, locale))
                .orElseThrow(() -> new ResourceNotFoundException("error.estadoanimo.notfound", id));
    }

    @Override
    public EstadoAnimoDTO actualizar(Long id, EstadoAnimoRequest request) {
        String locale = LocaleContextHolder.getLocale().getLanguage();
        EstadoAnimo estado = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("error.estadoanimo.notfound", id));

        estado.setCodigo(request.codigo());
        estado.setEmoji(request.emoji());
        estado.setIconUrl(request.iconUrl());
        estado.setMusicaUrl(request.musicaUrl());
        estado.setImagenUrl(request.imagenUrl());
        estado.setColorPrimario(request.colorPrimario());
        estado.setColorSecundario(request.colorSecundario());
        estado.setFontFamily(request.fontFamily());
        estado.setAnimationType(request.animationType());

        // Reemplazar todas las traducciones
        estado.getTraducciones().clear();
        request.traducciones().forEach(estado::addTraduccion);

        return EstadoAnimoDTO.fromEntity(repository.save(estado), locale);
    }

    @Override
    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("error.estadoanimo.notfound", id);
        }
        repository.deleteById(id);
    }
}
