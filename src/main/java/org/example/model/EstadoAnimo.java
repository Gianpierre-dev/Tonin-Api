package org.example.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "estados_animo")
public class EstadoAnimo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Slug estable en minúsculas que identifica el estado (ej. "feliz"). Inmutable en uso normal. */
    @Column(unique = true, nullable = false, length = 50)
    private String codigo;

    @Column(length = 10)
    private String emoji;

    @Column(length = 1000)
    private String iconUrl;

    @Column(length = 1000)
    private String musicaUrl;

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

    /** Traducciones del nombre para cada locale soportado. */
    @OneToMany(mappedBy = "estadoAnimo", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<EstadoAnimoTraduccion> traducciones = new ArrayList<>();

    public EstadoAnimo() {}

    public EstadoAnimo(String codigo, String emoji, String iconUrl, String musicaUrl, String imagenUrl,
                       String colorPrimario, String colorSecundario, String fontFamily, String animationType) {
        this.codigo = codigo;
        this.emoji = emoji;
        this.iconUrl = iconUrl;
        this.musicaUrl = musicaUrl;
        this.imagenUrl = imagenUrl;
        this.colorPrimario = colorPrimario;
        this.colorSecundario = colorSecundario;
        this.fontFamily = fontFamily;
        this.animationType = animationType;
    }

    /**
     * Agrega una traducción del nombre para el locale indicado.
     * Mantiene la relación bidireccional con EstadoAnimoTraduccion.
     */
    public void addTraduccion(String locale, String nombre) {
        traducciones.add(new EstadoAnimoTraduccion(this, locale, nombre));
    }

    /**
     * Devuelve el nombre traducido al locale solicitado.
     * Si no existe, hace fallback a "es"; si tampoco existe, devuelve null.
     */
    public String resolverNombre(String locale) {
        return traducciones.stream()
                .filter(t -> t.getLocale().equalsIgnoreCase(locale))
                .map(EstadoAnimoTraduccion::getNombre)
                .findFirst()
                .orElseGet(() -> traducciones.stream()
                        .filter(t -> t.getLocale().equalsIgnoreCase("es"))
                        .map(EstadoAnimoTraduccion::getNombre)
                        .findFirst()
                        .orElse(null));
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }
    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }
    public String getMusicaUrl() { return musicaUrl; }
    public void setMusicaUrl(String musicaUrl) { this.musicaUrl = musicaUrl; }
    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }
    public String getColorPrimario() { return colorPrimario; }
    public void setColorPrimario(String colorPrimario) { this.colorPrimario = colorPrimario; }
    public String getColorSecundario() { return colorSecundario; }
    public void setColorSecundario(String colorSecundario) { this.colorSecundario = colorSecundario; }
    public String getFontFamily() { return fontFamily; }
    public void setFontFamily(String fontFamily) { this.fontFamily = fontFamily; }
    public String getAnimationType() { return animationType; }
    public void setAnimationType(String animationType) { this.animationType = animationType; }
    public List<EstadoAnimoTraduccion> getTraducciones() { return traducciones; }
    public void setTraducciones(List<EstadoAnimoTraduccion> traducciones) { this.traducciones = traducciones; }
}
