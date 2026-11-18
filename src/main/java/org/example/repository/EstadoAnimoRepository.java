package org.example.repository;

import org.example.model.EstadoAnimo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface EstadoAnimoRepository extends JpaRepository<EstadoAnimo, Long> {
    Optional<EstadoAnimo> findByCodigoIgnoreCase(String codigo);
    boolean existsByCodigoIgnoreCase(String codigo);

    @Query("SELECT COUNT(e) > 0 FROM EstadoAnimo e WHERE e.iconUrl = :url OR e.musicaUrl = :url OR e.imagenUrl = :url")
    boolean existsByArchivoUrl(@Param("url") String url);

    @Query("SELECT e.iconUrl FROM EstadoAnimo e WHERE e.iconUrl IS NOT NULL "
            + "UNION SELECT e.musicaUrl FROM EstadoAnimo e WHERE e.musicaUrl IS NOT NULL "
            + "UNION SELECT e.imagenUrl FROM EstadoAnimo e WHERE e.imagenUrl IS NOT NULL")
    Set<String> findArchivoUrlsEnUso();
}
