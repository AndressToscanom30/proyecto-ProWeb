package com.cronos.gestiontributaria.mensajes.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;

/**
 * Entidad que representa un mensaje interno entre usuarios del sistema.
 *
 * <p>Los mensajes se almacenan en la colección {@code mensajes} de MongoDB
 * y permiten la comunicación directa entre usuarios registrados.</p>
 */
@Document(collection = "mensajes")
public class Mensaje {

    @Id
    private String id;

    /** Correo (username) del usuario que envió el mensaje. */
    private String emisorUsername;

    /** Correo (username) del usuario destinatario del mensaje. */
    private String receptorUsername;

    /** Asunto o título breve del mensaje. */
    @NotBlank(message = "El asunto es obligatorio")
    private String asunto;

    /** Cuerpo o contenido del mensaje. */
    @NotBlank(message = "El contenido es obligatorio")
    private String contenido;

    /** Indica si el receptor ya ha leído el mensaje. */
    private boolean leido = false;

    /** Fecha y hora de envío, generada automáticamente al crear el mensaje. */
    private LocalDateTime fechaEnvio;

    public Mensaje() {
        this.leido = false;
    }

    // ── Getters y Setters ──────────────────────────────────────────────

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmisorUsername() {
        return emisorUsername;
    }

    public void setEmisorUsername(String emisorUsername) {
        this.emisorUsername = emisorUsername;
    }

    public String getReceptorUsername() {
        return receptorUsername;
    }

    public void setReceptorUsername(String receptorUsername) {
        this.receptorUsername = receptorUsername;
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
