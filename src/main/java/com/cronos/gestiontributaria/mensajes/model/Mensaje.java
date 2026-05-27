package com.cronos.gestiontributaria.mensajes.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;

/**
 * Entidad que representa un mensaje interno entre usuarios del sistema.
 */
@Document(collection = "mensajes")
public class Mensaje {

    @Id
    private String id;

    private String emisorEmail;

    private String receptorEmail;

    @NotBlank
    private String asunto;

    @NotBlank
    private String contenido;

    private boolean leido = false;

    private LocalDateTime fechaEnvio;

    public Mensaje() {
    }

    // ── Getters y Setters ──

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmisorEmail() {
        return emisorEmail;
    }

    public void setEmisorEmail(String emisorEmail) {
        this.emisorEmail = emisorEmail;
    }

    public String getReceptorEmail() {
        return receptorEmail;
    }

    public void setReceptorEmail(String receptorEmail) {
        this.receptorEmail = receptorEmail;
    }

    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public boolean isLeido() {
        return leido;
    }

    public void setLeido(boolean leido) {
        this.leido = leido;
    }

    public LocalDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(LocalDateTime fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }
}
