package com.cronos.gestiontributaria.mensajes.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de entrada para crear un mensaje interno.
 *
 * <p>Recibe el nombre de usuario (correo) del destinatario, el asunto
 * y el contenido del mensaje. Todos los campos son obligatorios.</p>
 */
public class MensajeRequest {

    /** Correo (username) del destinatario. */
    @NotBlank(message = "El destinatario es obligatorio")
    private String destinatarioUsername;

    /** Asunto o título breve del mensaje. */
    @NotBlank(message = "El asunto es obligatorio")
    private String asunto;

    /** Cuerpo o contenido del mensaje. */
    @NotBlank(message = "El contenido es obligatorio")
    private String contenido;

    public MensajeRequest() {
    }

    public MensajeRequest(String destinatarioUsername, String asunto, String contenido) {
        this.destinatarioUsername = destinatarioUsername;
        this.asunto = asunto;
        this.contenido = contenido;
    }

    // ── Getters y Setters ──────────────────────────────────────────────

    public String getDestinatarioUsername() {
        return destinatarioUsername;
    }

    public void setDestinatarioUsername(String destinatarioUsername) {
        this.destinatarioUsername = destinatarioUsername;
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
}
