package com.cronos.gestiontributaria.mensajes.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.cronos.gestiontributaria.auth.repository.UserRepository;
import com.cronos.gestiontributaria.mensajes.dto.MensajeRequest;
import com.cronos.gestiontributaria.mensajes.model.Mensaje;
import com.cronos.gestiontributaria.mensajes.repository.MensajeRepository;

/**
 * Servicio de negocio para el módulo de mensajería interna.
 *
 * <p>Gestiona el envío, consulta y marcado de mensajes entre usuarios
 * registrados en el sistema.</p>
 */
@Service
public class MensajeService {

    private final MensajeRepository mensajeRepository;
    private final UserRepository userRepository;

    public MensajeService(MensajeRepository mensajeRepository, UserRepository userRepository) {
        this.mensajeRepository = mensajeRepository;
        this.userRepository = userRepository;
    }

    /**
     * Envía un mensaje interno de un usuario a otro.
     *
     * <p>Valida que el destinatario exista en el sistema antes de persistir
     * el mensaje. La fecha de envío se genera automáticamente.</p>
     *
     * @param request datos del mensaje (destinatario, asunto, contenido)
     * @param emisorUsername correo del usuario emisor (autenticado)
     * @return mensaje persistido
     * @throws IllegalArgumentException si el destinatario no existe
     */
    public Mensaje enviarMensaje(MensajeRequest request, String emisorUsername) {
        if (emisorUsername.equalsIgnoreCase(request.getDestinatarioUsername())) {
            throw new IllegalArgumentException("No puedes enviarte un mensaje a ti mismo");
        }

        // Verificar que el destinatario exista en el sistema
        if (userRepository.findByEmail(request.getDestinatarioUsername()).isEmpty()) {
            throw new IllegalArgumentException("El destinatario no existe en el sistema");
        }

        Mensaje mensaje = new Mensaje();
        mensaje.setEmisorUsername(emisorUsername);
        mensaje.setReceptorUsername(request.getDestinatarioUsername());
        mensaje.setAsunto(request.getAsunto());
        mensaje.setContenido(request.getContenido());
        mensaje.setLeido(false);
        mensaje.setFechaEnvio(LocalDateTime.now());

        return mensajeRepository.save(mensaje);
    }

    /**
     * Obtiene la bandeja de entrada (mensajes recibidos) del usuario.
     *
     * @param receptorUsername correo del usuario autenticado
     * @return lista de mensajes recibidos ordenados por fecha descendente
     */
    public List<Mensaje> obtenerBandejaEntrada(String receptorUsername) {
        return mensajeRepository.findByReceptorUsernameOrderByFechaEnvioDesc(receptorUsername);
    }

    /**
     * Obtiene los mensajes enviados por el usuario.
     *
     * @param emisorUsername correo del usuario autenticado
     * @return lista de mensajes enviados ordenados por fecha descendente
     */
    public List<Mensaje> obtenerEnviados(String emisorUsername) {
        return mensajeRepository.findByEmisorUsernameOrderByFechaEnvioDesc(emisorUsername);
    }

    /**
     * Marca un mensaje como leído. Solo el receptor puede marcar sus propios mensajes.
     *
     * @param mensajeId ID del mensaje a marcar
     * @param username correo del usuario autenticado
     * @return el mensaje actualizado, o vacío si no existe o no pertenece al usuario
     */
    public Optional<Mensaje> marcarComoLeido(String mensajeId, String username) {
        Optional<Mensaje> optMensaje = mensajeRepository.findById(mensajeId);
        if (optMensaje.isEmpty()) {
            return Optional.empty();
        }

        Mensaje mensaje = optMensaje.get();
        // Solo el receptor puede marcar como leído
        if (!mensaje.getReceptorUsername().equals(username)) {
            return Optional.empty();
        }

        mensaje.setLeido(true);
        return Optional.of(mensajeRepository.save(mensaje));
    }

    /**
     * Cuenta los mensajes no leídos en la bandeja de entrada del usuario.
     *
     * @param receptorUsername correo del usuario autenticado
     * @return cantidad de mensajes no leídos
     */
    public long contarNoLeidos(String receptorUsername) {
        return mensajeRepository.countByReceptorUsernameAndLeido(receptorUsername, false);
    }
}
