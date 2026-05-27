package com.cronos.gestiontributaria.mensajes.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import com.cronos.gestiontributaria.auth.model.User;
import com.cronos.gestiontributaria.auth.service.UserService;
import com.cronos.gestiontributaria.mensajes.model.Mensaje;
import com.cronos.gestiontributaria.mensajes.repository.MensajeRepository;

@Service
public class MensajeService {

    private final MensajeRepository mensajeRepository;
    private final UserService userService;

    public MensajeService(MensajeRepository mensajeRepository, UserService userService) {
        this.mensajeRepository = mensajeRepository;
        this.userService = userService;
    }

    public Mensaje enviarMensaje(String emisorEmail, String receptorEmail, String asunto, String contenido) {
        User receptor = userService.findByEmail(receptorEmail)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El usuario receptor con correo " + receptorEmail + " no existe"));

        Mensaje mensaje = new Mensaje(emisorEmail, receptor.getEmail(), asunto, contenido);
        return mensajeRepository.save(mensaje);
    }

    public List<Mensaje> obtenerBandejaEntrada(String email) {
        return mensajeRepository.findByReceptorEmailOrderByFechaEnvioDesc(email);
    }

    public List<Mensaje> obtenerEnviados(String email) {
        return mensajeRepository.findByEmisorEmailOrderByFechaEnvioDesc(email);
    }

    public Mensaje marcarComoLeido(String mensajeId, String emailUsuario) {
        Mensaje mensaje = mensajeRepository.findById(mensajeId)
                .orElseThrow(() -> new NoSuchElementException("Mensaje no encontrado con ID: " + mensajeId));

        if (!mensaje.getReceptorEmail().equals(emailUsuario)) {
            throw new SecurityException("No puedes marcar como leído un mensaje que no te pertenece");
        }

        mensaje.setLeido(true);
        return mensajeRepository.save(mensaje);
    }

    public long contarNoLeidos(String email) {
        return mensajeRepository.countByReceptorEmailAndLeidoFalse(email);
    }
}
