package org.example.model;

import jakarta.persistence.*;

/**
 * Traducción del nombre de un EstadoAnimo para un locale específico (ej. "es", "en").
 * La combinación (estado_animo_id, locale) es única.
 */
@Entity
@Table(
    name = "estado_animo_traduccion",
    uniqueConstraints = @UniqueConstraint(columnNames = {"estado_animo_id", "locale"})
)
public class EstadoAnimoTraduccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "estado_animo_id", nullable = false)
    private EstadoAnimo estadoAnimo;

    @Column(length = 5, nullable = false)
    private String locale;

    @Column(nullable = false)
    private String nombre;

    public EstadoAnimoTraduccion() {}

    public EstadoAnimoTraduccion(EstadoAnimo estadoAnimo, String locale, String nombre) {
        this.estadoAnimo = estadoAnimo;
        this.locale = locale;
        this.nombre = nombre;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public EstadoAnimo getEstadoAnimo() { return estadoAnimo; }
    public void setEstadoAnimo(EstadoAnimo estadoAnimo) { this.estadoAnimo = estadoAnimo; }
    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
