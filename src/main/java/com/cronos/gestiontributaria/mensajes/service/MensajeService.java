package com.cronos.gestiontributaria.mensajes.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.cronos.gestiontributaria.mensajes.dto.EnviarMensajeRequest;
import com.cronos.gestiontributaria.mensajes.model.Mensaje;
import com.cronos.gestiontributaria.mensajes.repository.MensajeRepository;

/**
 * Servicio de lógica de negocio para el sistema de mensajes internos.
 */
@Service
public class MensajeService {

    private final MensajeRepository mensajeRepository;

    public MensajeService(MensajeRepository mensajeRepository) {
        this.mensajeRepository = mensajeRepository;
    }

    /**
     * Envía un nuevo mensaje interno.
     *
     * @param emisorEmail email del usuario emisor
     * @param request     datos del mensaje a enviar
     * @return el mensaje persistido
     */
    public Mensaje enviar(String emisorEmail, EnviarMensajeRequest request) {
        Mensaje mensaje = new Mensaje();
        mensaje.setEmisorEmail(emisorEmail);
        mensaje.setReceptorEmail(request.getReceptorEmail());
        mensaje.setAsunto(request.getAsunto());
        mensaje.setContenido(request.getContenido());
        mensaje.setLeido(false);
        mensaje.setFechaEnvio(LocalDateTime.now());
        return mensajeRepository.save(mensaje);
    }

    /**
     * Obtiene la bandeja de entrada de un usuario.
     *
     * @param receptorEmail email del receptor
     * @return lista de mensajes recibidos ordenados por fecha descendente
     */
    public List<Mensaje> getBandejaEntrada(String receptorEmail) {
        return mensajeRepository.findByReceptorEmailOrderByFechaEnvioDesc(receptorEmail);
    }

    /**
     * Obtiene los mensajes enviados por un usuario.
     *
     * @param emisorEmail email del emisor
     * @return lista de mensajes enviados ordenados por fecha descendente
     */
    public List<Mensaje> getEnviados(String emisorEmail) {
        return mensajeRepository.findByEmisorEmailOrderByFechaEnvioDesc(emisorEmail);
    }

    /**
     * Marca un mensaje como leído. Solo el receptor puede hacerlo.
     *
     * @param id             id del mensaje
     * @param receptorEmail  email del usuario que solicita marcar como leído
     * @return el mensaje actualizado
     * @throws ResponseStatusException 404 si no existe, 403 si no es el receptor
     */
    public Mensaje marcarLeido(String id, String receptorEmail) {
        Mensaje mensaje = mensajeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!mensaje.getReceptorEmail().equals(receptorEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        mensaje.setLeido(true);
        return mensajeRepository.save(mensaje);
    }

    /**
     * Cuenta los mensajes no leídos de un receptor.
     *
     * @param receptorEmail email del receptor
     * @return cantidad de mensajes no leídos
     */
    public long countNoLeidos(String receptorEmail) {
        return mensajeRepository.countByReceptorEmailAndLeidoFalse(receptorEmail);
    }
}
