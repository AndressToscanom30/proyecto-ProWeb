package com.cronos.gestiontributaria.solicitudes.dto;

import com.cronos.gestiontributaria.solicitudes.model.TipoSolicitud;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para la solicitud de radicación de una nueva solicitud.
 */
public class RadicarSolicitudRequest {

    @NotNull
    private TipoSolicitud tipo;

    @NotBlank
    private String descripcion;

    public RadicarSolicitudRequest() {
    }

    // ── Getters y Setters ──

    public TipoSolicitud getTipo() {
        return tipo;
    }

    public void setTipo(TipoSolicitud tipo) {
        this.tipo = tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
