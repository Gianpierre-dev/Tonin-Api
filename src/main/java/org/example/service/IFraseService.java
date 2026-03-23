package org.example.service;

import org.example.dto.FraseDTO;
import org.example.dto.FraseRequest;

import java.util.List;
import java.util.Optional;

public interface IFraseService {

    FraseDTO guardar(FraseRequest request);

    List<FraseDTO> obtenerTodas();

    FraseDTO obtenerPorId(Long id);

    FraseDTO actualizar(Long id, FraseRequest request);

    void eliminar(Long id);

    Optional<FraseDTO> obtenerFraseAleatoria(String nombreAnimo, List<Long> idsExcluidos);
}
