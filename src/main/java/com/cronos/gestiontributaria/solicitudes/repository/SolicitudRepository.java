package com.cronos.gestiontributaria.solicitudes.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.cronos.gestiontributaria.solicitudes.model.Solicitud;

/**
 * Repositorio MongoDB para la entidad {@link Solicitud}.
 */
public interface SolicitudRepository extends MongoRepository<Solicitud, String> {

    List<Solicitud> findBySolicitanteEmailOrderByFechaCreacionDesc(String solicitanteEmail);
}
