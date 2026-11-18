package org.example.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
     * Deja las traducciones iguales al mapa recibido (locale -> texto): actualiza las existentes,
     * agrega las nuevas y quita las que ya no vienen. Actualiza en sitio en lugar de borrar y
     * volver a insertar, para no violar la restricción única (frase_id, locale).
     */
    public void sincronizarTraducciones(Map<String, String> nuevas) {
        traducciones.removeIf(t -> !nuevas.containsKey(t.getLocale()));
        nuevas.forEach((locale, texto) -> traducciones.stream()
                .filter(t -> t.getLocale().equals(locale))
                .findFirst()
                .ifPresentOrElse(t -> t.setTexto(texto), () -> addTraduccion(locale, texto)));
    }

    /**
     * Devuelve el texto traducido al locale solicitado.
     * Si no existe o está en blanco, hace fallback a "es"; si tampoco existe, devuelve null.
     */
    public String resolverTexto(String locale) {
        return textoPara(locale).orElseGet(() -> textoPara("es").orElse(null));
    }

    private Optional<String> textoPara(String locale) {
        return traducciones.stream()
                .filter(t -> t.getLocale().equalsIgnoreCase(locale))
                .map(FraseTraduccion::getTexto)
                .filter(texto -> texto != null && !texto.isBlank())
                .findFirst();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public EstadoAnimo getEstadoAnimo() { return estadoAnimo; }
    public void setEstadoAnimo(EstadoAnimo estadoAnimo) { this.estadoAnimo = estadoAnimo; }
    public List<FraseTraduccion> getTraducciones() { return traducciones; }
    public void setTraducciones(List<FraseTraduccion> traducciones) { this.traducciones = traducciones; }
}
