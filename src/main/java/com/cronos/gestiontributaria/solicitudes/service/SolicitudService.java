package com.cronos.gestiontributaria.solicitudes.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.cronos.gestiontributaria.solicitudes.dto.RadicarSolicitudRequest;
import com.cronos.gestiontributaria.solicitudes.model.EstadoSolicitud;
import com.cronos.gestiontributaria.solicitudes.model.Solicitud;
import com.cronos.gestiontributaria.solicitudes.repository.SolicitudRepository;

/**
 * Servicio de lógica de negocio para el sistema de solicitudes.
 */
@Service
public class SolicitudService {

    private final SolicitudRepository solicitudRepository;

    public SolicitudService(SolicitudRepository solicitudRepository) {
        this.solicitudRepository = solicitudRepository;
    }

    /**
     * Radica una nueva solicitud con estado PENDIENTE.
     *
     * @param solicitanteEmail email del usuario que radica
     * @param request          datos de la solicitud
     * @return la solicitud persistida
     */
    public Solicitud radicar(String solicitanteEmail, RadicarSolicitudRequest request) {
        Solicitud solicitud = new Solicitud();
        solicitud.setSolicitanteEmail(solicitanteEmail);
        solicitud.setTipo(request.getTipo());
        solicitud.setDescripcion(request.getDescripcion());
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        solicitud.setObservacion(null);
        solicitud.setFechaCreacion(LocalDateTime.now());
        solicitud.setFechaResolucion(null);
        return solicitudRepository.save(solicitud);
    }

    /**
     * Obtiene las solicitudes de un usuario específico.
     *
     * @param solicitanteEmail email del solicitante
     * @return lista de solicitudes ordenadas por fecha descendente
     */
    public List<Solicitud> getMisSolicitudes(String solicitanteEmail) {
        return solicitudRepository.findBySolicitanteEmailOrderByFechaCreacionDesc(solicitanteEmail);
    }

    /**
     * Obtiene todas las solicitudes del sistema (solo para ADMIN).
     *
     * @return lista completa de solicitudes
     */
    public List<Solicitud> getTodasLasSolicitudes() {
        return solicitudRepository.findAll();
    }

    /**
     * Aprueba una solicitud pendiente.
     *
     * @param id          id de la solicitud
     * @param observacion observación del administrador
     * @return la solicitud actualizada
     * @throws ResponseStatusException 404 si no existe
     */
    public Solicitud aprobar(String id, String observacion) {
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        solicitud.setEstado(EstadoSolicitud.APROBADA);
        solicitud.setObservacion(observacion);
        solicitud.setFechaResolucion(LocalDateTime.now());
        return solicitudRepository.save(solicitud);
    }

    /**
     * Rechaza una solicitud pendiente.
     *
     * @param id          id de la solicitud
     * @param observacion observación del administrador
     * @return la solicitud actualizada
     * @throws ResponseStatusException 404 si no existe
     */
    public Solicitud rechazar(String id, String observacion) {
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        solicitud.setEstado(EstadoSolicitud.RECHAZADA);
        solicitud.setObservacion(observacion);
        solicitud.setFechaResolucion(LocalDateTime.now());
        return solicitudRepository.save(solicitud);
    }
}
