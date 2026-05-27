package com.cronos.gestiontributaria.mensajes.controller;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.cronos.gestiontributaria.mensajes.dto.CrearMensajeRequest;
import com.cronos.gestiontributaria.mensajes.model.Mensaje;
import com.cronos.gestiontributaria.mensajes.service.MensajeService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mensajes")
public class MensajeController {

    private final MensajeService mensajeService;

    public MensajeController(MensajeService mensajeService) {
        this.mensajeService = mensajeService;
    }

    @PostMapping
    public ResponseEntity<Mensaje> enviarMensaje(
            @Valid @RequestBody CrearMensajeRequest request,
            Authentication authentication) {
        try {
            Mensaje mensaje = mensajeService.enviarMensaje(
                    authentication.getName(),
                    request.getReceptorEmail(),
                    request.getAsunto(),
                    request.getContenido());
            return ResponseEntity.status(HttpStatus.CREATED).body(mensaje);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/bandeja-entrada")
    public ResponseEntity<List<Mensaje>> obtenerBandejaEntrada(Authentication authentication) {
        List<Mensaje> mensajes = mensajeService.obtenerBandejaEntrada(authentication.getName());
        return ResponseEntity.ok(mensajes);
    }

    @GetMapping("/enviados")
    public ResponseEntity<List<Mensaje>> obtenerEnviados(Authentication authentication) {
        List<Mensaje> mensajes = mensajeService.obtenerEnviados(authentication.getName());
        return ResponseEntity.ok(mensajes);
    }

    @PutMapping("/{id}/leer")
    public ResponseEntity<Mensaje> marcarComoLeido(
            @PathVariable String id,
            Authentication authentication) {
        try {
            Mensaje mensaje = mensajeService.marcarComoLeido(id, authentication.getName());
            return ResponseEntity.ok(mensaje);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (SecurityException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        }
    }

    @GetMapping("/no-leidos/count")
    public ResponseEntity<Map<String, Long>> contarNoLeidos(Authentication authentication) {
        long count = mensajeService.contarNoLeidos(authentication.getName());
        return ResponseEntity.ok(Map.of("count", count));
    }
}
