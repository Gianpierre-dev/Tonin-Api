package org.example.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/**
 * Entidad que representa un estado de ánimo de forma dinámica.
 * Permite añadir nuevos estados (con su emoji) desde el panel admin.
 */
@Entity
@Table(name = "estados_animo")
public class EstadoAnimo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre del estado no puede estar vacío")
    @Column(unique = true, nullable = false)
    private String nombre;

    @Column(length = 10)
    private String emoji; // Guardaremos el emoji directamente (ej: "😊")

    @Column(length = 1000)
    private String musicaUrl; // URL de la canción de fondo

    @Column(length = 1000)
    private String imagenUrl;

    @Column(length = 7)
    private String colorPrimario;

    @Column(length = 7)
    private String colorSecundario;

    @Column(length = 50)
    private String fontFamily;

    @Column(length = 30)
    private String animationType;

    public EstadoAnimo() {
    }

    public EstadoAnimo(String nombre, String emoji, String musicaUrl, String imagenUrl,
                       String colorPrimario, String colorSecundario, String fontFamily, String animationType) {
        this.nombre = nombre;
        this.emoji = emoji;
        this.musicaUrl = musicaUrl;
        this.imagenUrl = imagenUrl;
        this.colorPrimario = colorPrimario;
        this.colorSecundario = colorSecundario;
        this.fontFamily = fontFamily;
        this.animationType = animationType;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public String getMusicaUrl() {
        return musicaUrl;
    }

    public void setMusicaUrl(String musicaUrl) {
        this.musicaUrl = musicaUrl;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    public String getColorPrimario() {
        return colorPrimario;
    }

    public void setColorPrimario(String colorPrimario) {
        this.colorPrimario = colorPrimario;
    }

    public String getColorSecundario() {
        return colorSecundario;
    }

    public void setColorSecundario(String colorSecundario) {
        this.colorSecundario = colorSecundario;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }

    public String getAnimationType() {
        return animationType;
    }

    public void setAnimationType(String animationType) {
        this.animationType = animationType;
    }
}
