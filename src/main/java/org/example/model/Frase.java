package org.example.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Entidad que representa una frase en el sistema.
 * Ahora se relaciona dinámicamente con un EstadoAnimo.
 */
@Entity
@Table(name = "frases")
public class Frase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El texto de la frase no puede estar vacío")
    @Size(min = 5, max = 500, message = "La frase debe tener entre 5 y 500 caracteres")
    @Column(nullable = false, length = 500)
    private String texto;

    @NotNull(message = "Debes asignar un estado de ánimo")
    @ManyToOne // Muchas frases pueden pertenecer a un mismo estado de ánimo
    @JoinColumn(name = "estado_animo_id", nullable = false)
    private EstadoAnimo estadoAnimo;

    public Frase() {
    }

    public Frase(String texto, EstadoAnimo estadoAnimo) {
        this.texto = texto;
        this.estadoAnimo = estadoAnimo;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public EstadoAnimo getEstadoAnimo() {
        return estadoAnimo;
    }

    public void setEstadoAnimo(EstadoAnimo estadoAnimo) {
        this.estadoAnimo = estadoAnimo;
    }
}
