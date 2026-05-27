package com.cronos.gestiontributaria.mensajes.dto;

import jakarta.validation.constraints.NotBlank;

public class CrearMensajeRequest {

    @NotBlank(message = "El correo del receptor es obligatorio")
    private String receptorEmail;

    @NotBlank(message = "El asunto es obligatorio")
    private String asunto;

    @NotBlank(message = "El contenido es obligatorio")
    private String contenido;

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
