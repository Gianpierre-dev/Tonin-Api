package org.example.service;

import org.example.dto.EstadoAnimoDTO;
import org.example.dto.EstadoAnimoRequest;
import org.example.exception.BadRequestException;
import org.example.exception.ResourceNotFoundException;
import org.example.model.EstadoAnimo;
import org.example.repository.EstadoAnimoRepository;
import org.example.repository.FraseRepository;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class EstadoAnimoService implements IEstadoAnimoService {

    private final EstadoAnimoRepository repository;
    private final FraseRepository fraseRepository;

    public EstadoAnimoService(EstadoAnimoRepository repository, FraseRepository fraseRepository) {
        this.repository = repository;
        this.fraseRepository = fraseRepository;
    }

    @Override
    @Transactional
    public EstadoAnimoDTO guardar(EstadoAnimoRequest request) {
        String locale = LocaleContextHolder.getLocale().getLanguage();
        Map<String, String> traducciones = Traducciones.normalizarYValidar(request.traducciones());
        if (repository.existsByCodigoIgnoreCase(request.codigo())) {
            throw new BadRequestException("error.codigo.exists");
        }
        EstadoAnimo estado = new EstadoAnimo(
                request.codigo(), request.emoji(), request.iconUrl(), request.musicaUrl(), request.imagenUrl(),
                request.colorPrimario(), request.colorSecundario(), request.fontFamily(), request.animationType()
        );
        traducciones.forEach(estado::addTraduccion);
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
    @Transactional
    public EstadoAnimoDTO actualizar(Long id, EstadoAnimoRequest request) {
        String locale = LocaleContextHolder.getLocale().getLanguage();
        Map<String, String> traducciones = Traducciones.normalizarYValidar(request.traducciones());
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
        estado.sincronizarTraducciones(traducciones);

        return EstadoAnimoDTO.fromEntity(repository.save(estado), locale);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("error.estadoanimo.notfound", id);
        }
        // Borrar un estado no debe arrastrar sus frases: se exige reasignarlas o borrarlas antes.
        long frases = fraseRepository.countByEstadoAnimoId(id);
        if (frases > 0) {
            throw new BadRequestException("error.estadoanimo.hasfrases", frases);
        }
        repository.deleteById(id);
    }
}
