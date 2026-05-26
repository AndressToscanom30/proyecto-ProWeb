package com.cronos.gestiontributaria.solicitudes.model;

/**
 * Estados posibles en el ciclo de vida de una solicitud.
 *
 * <p>Una solicitud inicia en {@code PENDIENTE} y solo un administrador
 * puede transicionarla a {@code APROBADA} o {@code RECHAZADA}.</p>
 */
public enum EstadoSolicitud {
    PENDIENTE,
    APROBADA,
    RECHAZADA
}
