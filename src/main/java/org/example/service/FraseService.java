package org.example.service;

import org.example.dto.FraseDTO;
import org.example.dto.FraseRequest;
import org.example.exception.ResourceNotFoundException;
import org.example.model.EstadoAnimo;
import org.example.model.Frase;
import org.example.repository.EstadoAnimoRepository;
import org.example.repository.FraseRepository;
import org.springframework.stereotype.Service;

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
                .orElseThrow(() -> new ResourceNotFoundException("error.estadoanimo.notfound", request.estadoAnimoId()));

        Frase frase = new Frase(request.texto(), estado);
        return FraseDTO.fromEntity(repository.save(frase));
    }

    @Override
    public List<FraseDTO> obtenerTodas() {
        return repository.findAll().stream()
                .map(FraseDTO::fromEntity)
                .toList();
    }

    @Override
    public FraseDTO obtenerPorId(Long id) {
        return repository.findById(id)
                .map(FraseDTO::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("error.frase.notfound", id));
    }

    @Override
    public FraseDTO actualizar(Long id, FraseRequest request) {
        Frase frase = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("error.frase.notfound", id));

        EstadoAnimo estado = estadoRepository.findById(request.estadoAnimoId())
                .orElseThrow(() -> new ResourceNotFoundException("error.estadoanimo.notfound", request.estadoAnimoId()));

        frase.setTexto(request.texto());
        frase.setEstadoAnimo(estado);
        return FraseDTO.fromEntity(repository.save(frase));
    }

    @Override
    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("error.frase.notfound", id);
        }
        repository.deleteById(id);
    }

    @Override
    public Optional<FraseDTO> obtenerFraseAleatoria(String nombreAnimo, List<Long> idsExcluidos) {
        EstadoAnimo estado = estadoRepository.findByNombre(nombreAnimo)
                .orElse(null);
        if (estado == null) return Optional.empty();

        Optional<Frase> frase = idsExcluidos.isEmpty()
                ? repository.findRandomByEstadoAnimo(estado)
                : repository.findRandomByEstadoAnimoAndIdNotIn(estado, idsExcluidos);

        return frase.map(FraseDTO::fromEntity);
    }
}
