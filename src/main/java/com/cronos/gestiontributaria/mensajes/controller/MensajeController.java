package com.cronos.gestiontributaria.mensajes.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cronos.gestiontributaria.mensajes.dto.EnviarMensajeRequest;
import com.cronos.gestiontributaria.mensajes.model.Mensaje;
import com.cronos.gestiontributaria.mensajes.service.MensajeService;

import jakarta.validation.Valid;

/**
 * Controlador REST para el sistema de mensajes internos.
 */
@RestController
@RequestMapping("/api/mensajes")
@PreAuthorize("isAuthenticated()")
public class MensajeController {

    private final MensajeService mensajeService;

    public MensajeController(MensajeService mensajeService) {
        this.mensajeService = mensajeService;
    }

    private String currentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mensaje enviar(@Valid @RequestBody EnviarMensajeRequest request) {
        return mensajeService.enviar(currentUserEmail(), request);
    }

    @GetMapping("/bandeja-entrada")
    public List<Mensaje> getBandejaEntrada() {
        return mensajeService.getBandejaEntrada(currentUserEmail());
    }

    @GetMapping("/enviados")
    public List<Mensaje> getEnviados() {
        return mensajeService.getEnviados(currentUserEmail());
    }

    @PutMapping("/{id}/leer")
    public Mensaje marcarLeido(@PathVariable String id) {
        return mensajeService.marcarLeido(id, currentUserEmail());
    }

    @GetMapping("/no-leidos/count")
    public Map<String, Long> countNoLeidos() {
        return Map.of("count", mensajeService.countNoLeidos(currentUserEmail()));
    }
}
