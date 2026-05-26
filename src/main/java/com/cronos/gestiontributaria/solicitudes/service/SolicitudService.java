package com.cronos.gestiontributaria.solicitudes.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.cronos.gestiontributaria.solicitudes.dto.SolicitudRequest;
import com.cronos.gestiontributaria.solicitudes.model.EstadoSolicitud;
import com.cronos.gestiontributaria.solicitudes.model.Solicitud;
import com.cronos.gestiontributaria.solicitudes.repository.SolicitudRepository;

/**
 * Servicio de negocio para el módulo de solicitudes con flujo de estados.
 *
 * <p>Gestiona la creación de solicitudes, consultas por usuario o globales,
 * y las transiciones de estado (aprobar/rechazar) por parte de administradores.</p>
 */
@Service
public class SolicitudService {

    private final SolicitudRepository solicitudRepository;

    public SolicitudService(SolicitudRepository solicitudRepository) {
        this.solicitudRepository = solicitudRepository;
    }

    /**
     * Radica una nueva solicitud con estado {@code PENDIENTE}.
     *
     * @param request datos de la solicitud (tipo y descripción)
     * @param solicitanteUsername correo del usuario que radica
     * @return solicitud persistida
     */
    public Solicitud crearSolicitud(SolicitudRequest request, String solicitanteUsername) {
        Solicitud solicitud = new Solicitud();
        solicitud.setSolicitanteUsername(solicitanteUsername);
        solicitud.setTipo(request.getTipo());
        solicitud.setDescripcion(request.getDescripcion());
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        solicitud.setFechaCreacion(LocalDateTime.now());
        return solicitudRepository.save(solicitud);
    }

    /**
     * Lista las solicitudes radicadas por un usuario específico.
     *
     * @param solicitanteUsername correo del usuario
     * @return solicitudes del usuario ordenadas por fecha descendente
     */
    public List<Solicitud> listarPorUsuario(String solicitanteUsername) {
        return solicitudRepository.findBySolicitanteUsernameOrderByFechaCreacionDesc(solicitanteUsername);
    }

    /**
     * Lista todas las solicitudes del sistema (acceso de administrador).
     *
     * @return todas las solicitudes
     */
    public List<Solicitud> listarTodas() {
        return solicitudRepository.findAll();
    }

    /**
     * Aprueba una solicitud: cambia estado a {@code APROBADA}, registra
     * la observación del administrador y la fecha de resolución.
     *
     * @param id ID de la solicitud
     * @param observacion observación del administrador
     * @return solicitud actualizada, o vacío si no existe
     */
    public Optional<Solicitud> aprobar(String id, String observacion) {
        Optional<Solicitud> optSolicitud = solicitudRepository.findById(id);
        if (optSolicitud.isEmpty()) {
            return Optional.empty();
        }

        Solicitud solicitud = optSolicitud.get();
        solicitud.setEstado(EstadoSolicitud.APROBADA);
        solicitud.setObservacion(observacion);
        solicitud.setFechaResolucion(LocalDateTime.now());
        return Optional.of(solicitudRepository.save(solicitud));
    }

    /**
     * Rechaza una solicitud: cambia estado a {@code RECHAZADA}, registra
     * la observación del administrador y la fecha de resolución.
     *
     * @param id ID de la solicitud
     * @param observacion observación del administrador
     * @return solicitud actualizada, o vacío si no existe
     */
    public Optional<Solicitud> rechazar(String id, String observacion) {
        Optional<Solicitud> optSolicitud = solicitudRepository.findById(id);
        if (optSolicitud.isEmpty()) {
            return Optional.empty();
        }

        Solicitud solicitud = optSolicitud.get();
        solicitud.setEstado(EstadoSolicitud.RECHAZADA);
        solicitud.setObservacion(observacion);
        solicitud.setFechaResolucion(LocalDateTime.now());
        return Optional.of(solicitudRepository.save(solicitud));
    }

    /**
     * Cuenta las solicitudes en un estado determinado.
     *
     * @param estado estado a contar
     * @return cantidad de solicitudes
     */
    public long contarPorEstado(EstadoSolicitud estado) {
        return solicitudRepository.countByEstado(estado);
    }

    /**
     * Cuenta el total de solicitudes en el sistema.
     *
     * @return total de solicitudes
     */
    public long contarTotal() {
        return solicitudRepository.count();
    }
}
