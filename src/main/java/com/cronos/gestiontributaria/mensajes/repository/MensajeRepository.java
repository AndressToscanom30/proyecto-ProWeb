package com.cronos.gestiontributaria.mensajes.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.cronos.gestiontributaria.mensajes.model.Mensaje;

/**
 * Repositorio MongoDB para consultar mensajes internos por emisor y receptor.
 */
public interface MensajeRepository extends MongoRepository<Mensaje, String> {

    /**
     * Obtiene todos los mensajes recibidos por un usuario, ordenados por fecha descendente.
     *
     * @param receptorUsername correo del receptor
     * @return lista de mensajes recibidos
     */
    List<Mensaje> findByReceptorUsernameOrderByFechaEnvioDesc(String receptorUsername);

    /**
     * Obtiene todos los mensajes enviados por un usuario, ordenados por fecha descendente.
     *
     * @param emisorUsername correo del emisor
     * @return lista de mensajes enviados
     */
    List<Mensaje> findByEmisorUsernameOrderByFechaEnvioDesc(String emisorUsername);

    /**
     * Cuenta los mensajes no leídos en la bandeja de entrada del usuario.
     *
     * @param receptorUsername correo del receptor
     * @param leido estado de lectura (false para no leídos)
     * @return cantidad de mensajes no leídos
     */
    long countByReceptorUsernameAndLeido(String receptorUsername, boolean leido);
}
