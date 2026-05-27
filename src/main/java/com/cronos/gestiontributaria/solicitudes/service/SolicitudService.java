package com.cronos.gestiontributaria.solicitudes.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import com.cronos.gestiontributaria.solicitudes.model.EstadoSolicitud;
import com.cronos.gestiontributaria.solicitudes.model.Solicitud;
import com.cronos.gestiontributaria.solicitudes.model.TipoSolicitud;
import com.cronos.gestiontributaria.solicitudes.repository.SolicitudRepository;

@Service
public class SolicitudService {

    private final SolicitudRepository solicitudRepository;

    public SolicitudService(SolicitudRepository solicitudRepository) {
        this.solicitudRepository = solicitudRepository;
    }

    public Solicitud radicar(String solicitanteEmail, TipoSolicitud tipo, String descripcion) {
        Solicitud solicitud = new Solicitud(solicitanteEmail, tipo, descripcion);
        return solicitudRepository.save(solicitud);
    }

    public List<Solicitud> obtenerMisSolicitudes(String email) {
        return solicitudRepository.findBySolicitanteEmailOrderByFechaCreacionDesc(email);
    }

    public List<Solicitud> obtenerTodas() {
        return solicitudRepository.findAllByOrderByFechaCreacionDesc();
    }

    public Solicitud aprobar(String solicitudId, String observacion) {
        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new NoSuchElementException("Solicitud no encontrada con ID: " + solicitudId));

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new IllegalStateException("La solicitud ya fue " + solicitud.getEstado().name().toLowerCase());
        }

        solicitud.setEstado(EstadoSolicitud.APROBADA);
        solicitud.setObservacion(observacion);
        solicitud.setFechaResolucion(LocalDateTime.now());
        return solicitudRepository.save(solicitud);
    }

    public Solicitud rechazar(String solicitudId, String observacion) {
        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new NoSuchElementException("Solicitud no encontrada con ID: " + solicitudId));

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new IllegalStateException("La solicitud ya fue " + solicitud.getEstado().name().toLowerCase());
        }

        solicitud.setEstado(EstadoSolicitud.RECHAZADA);
        solicitud.setObservacion(observacion);
        solicitud.setFechaResolucion(LocalDateTime.now());
        return solicitudRepository.save(solicitud);
    }

    public long contarTotal() {
        return solicitudRepository.count();
    }

    public long contarPorEstado(EstadoSolicitud estado) {
        return solicitudRepository.countByEstado(estado);
    }
}
