package com.cronos.gestiontributaria.solicitudes.dto;

import com.cronos.gestiontributaria.solicitudes.model.TipoSolicitud;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO de entrada para radicar una nueva solicitud.
 *
 * <p>El usuario indica el tipo de solicitud y una descripción. El estado
 * se asigna automáticamente como {@code PENDIENTE}.</p>
 */
public class SolicitudRequest {

    /** Tipo de solicitud: SOPORTE, ACCESO o INFORMACION. */
    @NotNull(message = "El tipo de solicitud es obligatorio")
    private TipoSolicitud tipo;

    /** Descripción detallada de lo que se solicita. */
    @NotBlank(message = "La descripción es obligatoria")
    private String descripcion;

    public SolicitudRequest() {
    }

    public SolicitudRequest(TipoSolicitud tipo, String descripcion) {
        this.tipo = tipo;
        this.descripcion = descripcion;
    }

    // ── Getters y Setters ──────────────────────────────────────────────

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
