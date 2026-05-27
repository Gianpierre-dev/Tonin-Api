package org.example.repository;

import org.example.model.EstadoAnimo;
import org.example.model.Frase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FraseRepository extends JpaRepository<Frase, Long> {

    List<Frase> findByEstadoAnimo(EstadoAnimo estadoAnimo);

    List<Frase> findByEstadoAnimoAndIdNotIn(EstadoAnimo estadoAnimo, List<Long> ids);

    @Query("SELECT f FROM Frase f WHERE f.estadoAnimo = :estado ORDER BY FUNCTION('RANDOM') LIMIT 1")
    Optional<Frase> findRandomByEstadoAnimo(@Param("estado") EstadoAnimo estado);

    @Query("SELECT f FROM Frase f WHERE f.estadoAnimo = :estado AND f.id NOT IN :ids ORDER BY FUNCTION('RANDOM') LIMIT 1")
    Optional<Frase> findRandomByEstadoAnimoAndIdNotIn(@Param("estado") EstadoAnimo estado, @Param("ids") List<Long> ids);
}
