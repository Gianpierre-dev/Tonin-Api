package org.example.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "frases")
public class Frase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "estado_animo_id", nullable = false)
    private EstadoAnimo estadoAnimo;

    /** Traducciones del texto para cada locale soportado. */
    @OneToMany(mappedBy = "frase", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<FraseTraduccion> traducciones = new ArrayList<>();

    public Frase() {}

    public Frase(EstadoAnimo estadoAnimo) {
        this.estadoAnimo = estadoAnimo;
    }

    /**
     * Agrega una traducción del texto para el locale indicado.
     * Mantiene la relación bidireccional con FraseTraduccion.
     */
    public void addTraduccion(String locale, String texto) {
        traducciones.add(new FraseTraduccion(this, locale, texto));
    }

    /**
     * Devuelve el texto traducido al locale solicitado.
     * Si no existe, hace fallback a "es"; si tampoco existe, devuelve null.
     */
    public String resolverTexto(String locale) {
        return traducciones.stream()
                .filter(t -> t.getLocale().equalsIgnoreCase(locale))
                .map(FraseTraduccion::getTexto)
                .findFirst()
                .orElseGet(() -> traducciones.stream()
                        .filter(t -> t.getLocale().equalsIgnoreCase("es"))
                        .map(FraseTraduccion::getTexto)
                        .findFirst()
                        .orElse(null));
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public EstadoAnimo getEstadoAnimo() { return estadoAnimo; }
    public void setEstadoAnimo(EstadoAnimo estadoAnimo) { this.estadoAnimo = estadoAnimo; }
    public List<FraseTraduccion> getTraducciones() { return traducciones; }
    public void setTraducciones(List<FraseTraduccion> traducciones) { this.traducciones = traducciones; }
}
