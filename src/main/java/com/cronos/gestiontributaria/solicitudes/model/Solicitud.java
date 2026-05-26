package com.cronos.gestiontributaria.solicitudes.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Entidad que representa una solicitud radicada por un usuario.
 *
 * <p>Cada solicitud tiene un ciclo de vida definido: inicia en estado
 * {@code PENDIENTE} y puede transicionar a {@code APROBADA} o {@code RECHAZADA}
 * por un administrador, quien debe registrar una observación al resolver.</p>
 */
@Document(collection = "solicitudes")
public class Solicitud {

    @Id
    private String id;

    /** Correo (username) del usuario que radicó la solicitud. */
    private String solicitanteUsername;

    /** Tipo de solicitud: SOPORTE, ACCESO o INFORMACIÓN. */
    @NotNull(message = "El tipo de solicitud es obligatorio")
    private TipoSolicitud tipo;

    /** Descripción detallada de lo que se solicita. */
    @NotBlank(message = "La descripción es obligatoria")
    private String descripcion;

    /** Estado actual de la solicitud en su ciclo de vida. */
    private EstadoSolicitud estado;

    /** Observación o respuesta del administrador al aprobar o rechazar. */
    private String observacion;

    /** Fecha y hora de creación, generada automáticamente. */
    private LocalDateTime fechaCreacion;

    /** Fecha y hora de resolución, registrada al aprobar o rechazar. */
    private LocalDateTime fechaResolucion;

    public Solicitud() {
        this.estado = EstadoSolicitud.PENDIENTE;
    }

    // ── Getters y Setters ──────────────────────────────────────────────

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSolicitanteUsername() {
        return solicitanteUsername;
    }

    public void setSolicitanteUsername(String solicitanteUsername) {
        this.solicitanteUsername = solicitanteUsername;
    }

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

    public EstadoSolicitud getEstado() {
        return estado;
    }

    public void setEstado(EstadoSolicitud estado) {
        this.estado = estado;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaResolucion() {
        return fechaResolucion;
    }

    public void setFechaResolucion(LocalDateTime fechaResolucion) {
        this.fechaResolucion = fechaResolucion;
    }
}
