package com.cronos.gestiontributaria.mensajes.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.cronos.gestiontributaria.mensajes.model.Mensaje;

/**
 * Repositorio MongoDB para la entidad {@link Mensaje}.
 */
public interface MensajeRepository extends MongoRepository<Mensaje, String> {

    List<Mensaje> findByReceptorEmailOrderByFechaEnvioDesc(String receptorEmail);

    List<Mensaje> findByEmisorEmailOrderByFechaEnvioDesc(String emisorEmail);

    long countByReceptorEmailAndLeidoFalse(String receptorEmail);
}
