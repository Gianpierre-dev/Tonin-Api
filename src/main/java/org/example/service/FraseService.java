package org.example.service;

import org.example.dto.FraseDTO;
import org.example.dto.FraseRequest;
import org.example.exception.BadRequestException;
import org.example.exception.ResourceNotFoundException;
import org.example.model.EstadoAnimo;
import org.example.model.Frase;
import org.example.repository.EstadoAnimoRepository;
import org.example.repository.FraseRepository;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class FraseService implements IFraseService {

    private static final String LOCALE_DEFAULT = "es";

    private final FraseRepository repository;
    private final EstadoAnimoRepository estadoRepository;

    public FraseService(FraseRepository repository, EstadoAnimoRepository estadoRepository) {
        this.repository = repository;
        this.estadoRepository = estadoRepository;
    }

    @Override
    public FraseDTO guardar(FraseRequest request) {
        String locale = LocaleContextHolder.getLocale().getLanguage();
        validarTraduccionEsRequerida(request.traducciones());
        EstadoAnimo estado = estadoRepository.findById(request.estadoAnimoId())
                .orElseThrow(() -> new ResourceNotFoundException("error.estadoanimo.notfound", request.estadoAnimoId()));

        Frase frase = new Frase(estado);
        request.traducciones().forEach(frase::addTraduccion);
        return FraseDTO.fromEntity(repository.save(frase), locale);
    }

    @Override
    public List<FraseDTO> obtenerTodas() {
        String locale = LocaleContextHolder.getLocale().getLanguage();
        return repository.findAll().stream()
                .map(f -> FraseDTO.fromEntity(f, locale))
                .toList();
    }

    @Override
    public FraseDTO obtenerPorId(Long id) {
        String locale = LocaleContextHolder.getLocale().getLanguage();
        return repository.findById(id)
                .map(f -> FraseDTO.fromEntity(f, locale))
                .orElseThrow(() -> new ResourceNotFoundException("error.frase.notfound", id));
    }

    @Override
    public FraseDTO actualizar(Long id, FraseRequest request) {
        String locale = LocaleContextHolder.getLocale().getLanguage();
        validarTraduccionEsRequerida(request.traducciones());
        Frase frase = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("error.frase.notfound", id));

        EstadoAnimo estado = estadoRepository.findById(request.estadoAnimoId())
                .orElseThrow(() -> new ResourceNotFoundException("error.estadoanimo.notfound", request.estadoAnimoId()));

        frase.setEstadoAnimo(estado);

        // Reemplazar todas las traducciones
        frase.getTraducciones().clear();
        request.traducciones().forEach(frase::addTraduccion);

        return FraseDTO.fromEntity(repository.save(frase), locale);
    }

    @Override
    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("error.frase.notfound", id);
        }
        repository.deleteById(id);
    }

    @Override
    public Optional<FraseDTO> obtenerFraseAleatoria(String codigoAnimo, List<Long> idsExcluidos) {
        String locale = LocaleContextHolder.getLocale().getLanguage();
        EstadoAnimo estado = estadoRepository.findByCodigoIgnoreCase(codigoAnimo).orElse(null);
        if (estado == null) return Optional.empty();

        Optional<Frase> frase = idsExcluidos.isEmpty()
                ? repository.findRandomByEstadoAnimo(estado)
                : repository.findRandomByEstadoAnimoAndIdNotIn(estado, idsExcluidos);

        return frase.map(f -> FraseDTO.fromEntity(f, locale));
    }

    private void validarTraduccionEsRequerida(Map<String, String> traducciones) {
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
