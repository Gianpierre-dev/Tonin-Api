package org.example.service;

import org.example.dto.EstadoAnimoDTO;
import org.example.dto.FraseDTO;
import org.example.dto.FraseRequest;
import org.example.exception.ResourceNotFoundException;
import org.example.model.EstadoAnimo;
import org.example.model.Frase;
import org.example.repository.EstadoAnimoRepository;
import org.example.repository.FraseRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class FraseService implements IFraseService {

    private final FraseRepository repository;
    private final EstadoAnimoRepository estadoRepository;

    public FraseService(FraseRepository repository, EstadoAnimoRepository estadoRepository) {
        this.repository = repository;
        this.estadoRepository = estadoRepository;
    }

    @Override
    public FraseDTO guardar(FraseRequest request) {
        EstadoAnimo estado = estadoRepository.findById(request.estadoAnimoId())
                .orElseThrow(() -> new ResourceNotFoundException("Estado de ánimo", request.estadoAnimoId()));

        Frase frase = new Frase(request.texto(), estado);
        return convertToDTO(repository.save(frase));
    }

    @Override
    public List<FraseDTO> obtenerTodas() {
        return repository.findAll().stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    public FraseDTO obtenerPorId(Long id) {
        return repository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Frase", id));
    }

    @Override
    public FraseDTO actualizar(Long id, FraseRequest request) {
        Frase frase = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Frase", id));

        EstadoAnimo estado = estadoRepository.findById(request.estadoAnimoId())
                .orElseThrow(() -> new ResourceNotFoundException("Estado de ánimo", request.estadoAnimoId()));

        frase.setTexto(request.texto());
        frase.setEstadoAnimo(estado);
        return convertToDTO(repository.save(frase));
    }

    @Override
    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Frase", id);
        }
        repository.deleteById(id);
    }

    @Override
    public Optional<FraseDTO> obtenerFraseAleatoria(String nombreAnimo, List<Long> idsExcluidos) {
        EstadoAnimo estado = estadoRepository.findByNombre(nombreAnimo)
                .orElse(null);
        if (estado == null) return Optional.empty();

        List<Frase> disponibles = idsExcluidos.isEmpty()
                ? repository.findByEstadoAnimo(estado)
                : repository.findByEstadoAnimoAndIdNotIn(estado, idsExcluidos);

        if (disponibles.isEmpty()) return Optional.empty();

        Collections.shuffle(disponibles);
        return Optional.of(convertToDTO(disponibles.getFirst()));
    }

    private FraseDTO convertToDTO(Frase frase) {
        EstadoAnimoDTO estadoDTO = new EstadoAnimoDTO(
                frase.getEstadoAnimo().getId(),
                frase.getEstadoAnimo().getNombre(),
                frase.getEstadoAnimo().getEmoji(),
                frase.getEstadoAnimo().getIconUrl(),
                frase.getEstadoAnimo().getMusicaUrl(),
                frase.getEstadoAnimo().getImagenUrl(),
                frase.getEstadoAnimo().getColorPrimario(),
                frase.getEstadoAnimo().getColorSecundario(),
                frase.getEstadoAnimo().getFontFamily(),
                frase.getEstadoAnimo().getAnimationType()
        );
        return new FraseDTO(frase.getId(), frase.getTexto(), estadoDTO);
    }
}
