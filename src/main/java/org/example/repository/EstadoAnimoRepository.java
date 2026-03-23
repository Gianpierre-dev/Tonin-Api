package org.example.repository;

import org.example.model.EstadoAnimo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstadoAnimoRepository extends JpaRepository<EstadoAnimo, Long> {
    Optional<EstadoAnimo> findByNombre(String nombre);
}
