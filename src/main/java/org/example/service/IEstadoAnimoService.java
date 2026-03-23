package org.example.service;

import org.example.dto.EstadoAnimoDTO;
import org.example.dto.EstadoAnimoRequest;

import java.util.List;

public interface IEstadoAnimoService {

    EstadoAnimoDTO guardar(EstadoAnimoRequest request);

    List<EstadoAnimoDTO> obtenerTodos();

    EstadoAnimoDTO obtenerPorId(Long id);

    EstadoAnimoDTO actualizar(Long id, EstadoAnimoRequest request);

    void eliminar(Long id);
}
