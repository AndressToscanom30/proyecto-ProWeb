package com.cronos.gestiontributaria.solicitudes.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.cronos.gestiontributaria.solicitudes.model.EstadoSolicitud;
import com.cronos.gestiontributaria.solicitudes.model.Solicitud;

/**
 * Repositorio MongoDB para consultar solicitudes por solicitante y estado.
 */
public interface SolicitudRepository extends MongoRepository<Solicitud, String> {

    /**
     * Obtiene las solicitudes radicadas por un usuario específico.
     *
     * @param solicitanteUsername correo del solicitante
     * @return lista de solicitudes del usuario
     */
    List<Solicitud> findBySolicitanteUsernameOrderByFechaCreacionDesc(String solicitanteUsername);

    /**
     * Cuenta las solicitudes que se encuentran en un estado determinado.
     *
     * @param estado estado a contar
     * @return cantidad de solicitudes en ese estado
     */
    long countByEstado(EstadoSolicitud estado);
}
