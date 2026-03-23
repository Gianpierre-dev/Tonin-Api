package org.example.repository;

import org.example.model.EstadoAnimo;
import org.example.model.Frase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FraseRepository extends JpaRepository<Frase, Long> {

    List<Frase> findByEstadoAnimo(EstadoAnimo estadoAnimo);

    List<Frase> findByEstadoAnimoAndIdNotIn(EstadoAnimo estadoAnimo, List<Long> ids);
}
