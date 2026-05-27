package com.cronos.gestiontributaria.solicitudes.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.cronos.gestiontributaria.solicitudes.model.EstadoSolicitud;
import com.cronos.gestiontributaria.solicitudes.model.Solicitud;

public interface SolicitudRepository extends MongoRepository<Solicitud, String> {

    List<Solicitud> findBySolicitanteEmailOrderByFechaCreacionDesc(String solicitanteEmail);

    List<Solicitud> findAllByOrderByFechaCreacionDesc();

    long countByEstado(EstadoSolicitud estado);
}
