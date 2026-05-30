package org.example.service;

import org.example.dto.EstadoAnimoDTO;
import org.example.dto.EstadoAnimoRequest;
import org.example.exception.BadRequestException;
import org.example.exception.ResourceNotFoundException;
import org.example.model.EstadoAnimo;
import org.example.repository.EstadoAnimoRepository;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EstadoAnimoService implements IEstadoAnimoService {

    private static final String LOCALE_DEFAULT = "es";

    private final EstadoAnimoRepository repository;

    public EstadoAnimoService(EstadoAnimoRepository repository) {
        this.repository = repository;
    }

    @Override
    public EstadoAnimoDTO guardar(EstadoAnimoRequest request) {
        String locale = LocaleContextHolder.getLocale().getLanguage();
        validarTraduccionEsRequerida(request.traducciones());
        if (repository.existsByCodigoIgnoreCase(request.codigo())) {
            throw new BadRequestException("error.codigo.exists");
        }
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
        validarTraduccionEsRequerida(request.traducciones());
        EstadoAnimo estado = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("error.estadoanimo.notfound", id));

        // El código es inmutable: si llega distinto al actual, rechazamos.
        if (!estado.getCodigo().equalsIgnoreCase(request.codigo())) {
            throw new BadRequestException("error.codigo.immutable");
        }

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

    private void validarTraduccionEsRequerida(java.util.Map<String, String> traducciones) {
        if (traducciones == null
                || !traducciones.containsKey(LOCALE_DEFAULT)
                || traducciones.get(LOCALE_DEFAULT) == null
                || traducciones.get(LOCALE_DEFAULT).isBlank()) {
            throw new BadRequestException("error.traducciones.esrequired");
        }
        for (String texto : traducciones.values()) {
            if (texto != null && texto.length() > 500) {
                throw new BadRequestException("error.traducciones.texto.size");
            }
        }
    }
}
