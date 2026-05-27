package com.cronos.gestiontributaria.mensajes.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO para la solicitud de envío de un mensaje interno.
 */
public class EnviarMensajeRequest {

    @NotBlank
    private String receptorEmail;

    @NotBlank
    private String asunto;

    @NotBlank
    private String contenido;

    public EnviarMensajeRequest() {
    }

    // ── Getters y Setters ──

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
}
