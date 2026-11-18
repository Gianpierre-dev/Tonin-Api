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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class FraseService implements IFraseService {

    /** Tope de IDs excluidos en la frase aleatoria (evita un NOT IN gigante). */
    static final int MAX_EXCLUIDOS = 500;

    private final FraseRepository repository;
    private final EstadoAnimoRepository estadoRepository;

    public FraseService(FraseRepository repository, EstadoAnimoRepository estadoRepository) {
        this.repository = repository;
        this.estadoRepository = estadoRepository;
    }

    @Override
    @Transactional
    public FraseDTO guardar(FraseRequest request) {
        String locale = LocaleContextHolder.getLocale().getLanguage();
        Map<String, String> traducciones = Traducciones.normalizarYValidar(request.traducciones(), Traducciones.MAX_TEXTO_FRASE);
        EstadoAnimo estado = estadoRepository.findById(request.estadoAnimoId())
                .orElseThrow(() -> new ResourceNotFoundException("error.estadoanimo.notfound", request.estadoAnimoId()));

        Frase frase = new Frase(estado);
        traducciones.forEach(frase::addTraduccion);
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
    @Transactional
    public FraseDTO actualizar(Long id, FraseRequest request) {
        String locale = LocaleContextHolder.getLocale().getLanguage();
        Map<String, String> traducciones = Traducciones.normalizarYValidar(request.traducciones(), Traducciones.MAX_TEXTO_FRASE);
        Frase frase = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("error.frase.notfound", id));

        EstadoAnimo estado = estadoRepository.findById(request.estadoAnimoId())
                .orElseThrow(() -> new ResourceNotFoundException("error.estadoanimo.notfound", request.estadoAnimoId()));

        frase.setEstadoAnimo(estado);
        frase.sincronizarTraducciones(traducciones);

        return FraseDTO.fromEntity(repository.save(frase), locale);
    }

    @Override
    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("error.frase.notfound", id);
        }
        repository.deleteById(id);
    }

    /**
     * Las tres situaciones sin frase responden 404, pero con mensajes distintos para que el
     * cliente sepa qué hacer:
     * <ul>
     *   <li>error.estadoanimo.codigo.notfound: el código de ánimo no existe;</li>
     *   <li>error.frase.sinfrases: el estado existe pero no tiene frases;</li>
     *   <li>error.frase.agotadas: todas sus frases están en {@code excluidos}; el cliente debe
     *       reiniciar la lista.</li>
     * </ul>
     */
    @Override
    public FraseDTO obtenerFraseAleatoria(String codigoAnimo, List<Long> idsExcluidos) {
        String locale = LocaleContextHolder.getLocale().getLanguage();
        // "?excluidos=1,,2" llega con nulos: se descartan (y los repetidos) antes del NOT IN.
        List<Long> excluidos = idsExcluidos == null ? List.of()
                : idsExcluidos.stream().filter(Objects::nonNull).distinct().toList();
        if (excluidos.size() > MAX_EXCLUIDOS) {
            throw new BadRequestException("error.excluidos.size", MAX_EXCLUIDOS);
        }

        EstadoAnimo estado = estadoRepository.findByCodigoIgnoreCase(codigoAnimo)
                .orElseThrow(() -> new ResourceNotFoundException("error.estadoanimo.codigo.notfound", codigoAnimo));

        Optional<Frase> frase = excluidos.isEmpty()
                ? repository.findRandomByEstadoAnimo(estado)
                : repository.findRandomByEstadoAnimoAndIdNotIn(estado, excluidos);

        return frase.map(f -> FraseDTO.fromEntity(f, locale))
                .orElseThrow(() -> excluidos.isEmpty()
                        ? new ResourceNotFoundException("error.frase.sinfrases", codigoAnimo)
                        : new ResourceNotFoundException("error.frase.agotadas", codigoAnimo));
    }
}
