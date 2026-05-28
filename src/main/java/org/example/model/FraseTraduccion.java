package org.example.model;

import jakarta.persistence.*;

/**
 * Traducción del texto de una Frase para un locale específico (ej. "es", "en").
 * La combinación (frase_id, locale) es única.
 */
@Entity
@Table(
    name = "frase_traduccion",
    uniqueConstraints = @UniqueConstraint(columnNames = {"frase_id", "locale"})
)
public class FraseTraduccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "frase_id", nullable = false)
    private Frase frase;

    @Column(length = 5, nullable = false)
    private String locale;

    @Column(nullable = false, length = 500)
    private String texto;

    public FraseTraduccion() {}

    public FraseTraduccion(Frase frase, String locale, String texto) {
        this.frase = frase;
        this.locale = locale;
        this.texto = texto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Frase getFrase() { return frase; }
    public void setFrase(Frase frase) { this.frase = frase; }
    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
}
