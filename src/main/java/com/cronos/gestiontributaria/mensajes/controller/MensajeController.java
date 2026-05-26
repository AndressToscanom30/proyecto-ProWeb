package com.cronos.gestiontributaria.mensajes.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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

import com.cronos.gestiontributaria.mensajes.dto.MensajeRequest;
import com.cronos.gestiontributaria.mensajes.model.Mensaje;
import com.cronos.gestiontributaria.mensajes.service.MensajeService;

import jakarta.validation.Valid;

/**
 * Controlador REST para el módulo de mensajería interna.
 *
 * <p>Expone endpoints para enviar mensajes, consultar bandeja de entrada,
 * mensajes enviados, marcar como leído y contar no leídos.</p>
 */
@RestController
@RequestMapping("/api/mensajes")
public class MensajeController {

    private final MensajeService mensajeService;

    public MensajeController(MensajeService mensajeService) {
        this.mensajeService = mensajeService;
    }

    /**
     * Crea y persiste un nuevo mensaje interno.
     *
     * @param request datos del mensaje (destinatario, asunto, contenido)
     * @param authentication información del usuario autenticado
     * @return mensaje creado con código 201 Created
     */
    @PostMapping
    public ResponseEntity<?> enviarMensaje(@Valid @RequestBody MensajeRequest request,
                                           Authentication authentication) {
        try {
            Mensaje mensaje = mensajeService.enviarMensaje(request, authentication.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(mensaje);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Lista los mensajes recibidos por el usuario autenticado (bandeja de entrada).
     *
     * @param authentication información del usuario autenticado
     * @return lista de mensajes recibidos con código 200 OK
     */
    @GetMapping("/bandeja-entrada")
    public ResponseEntity<List<Mensaje>> bandejaEntrada(Authentication authentication) {
        List<Mensaje> mensajes = mensajeService.obtenerBandejaEntrada(authentication.getName());
        return ResponseEntity.ok(mensajes);
    }

    /**
     * Lista los mensajes enviados por el usuario autenticado.
     *
     * @param authentication información del usuario autenticado
     * @return lista de mensajes enviados con código 200 OK
     */
    @GetMapping("/enviados")
    public ResponseEntity<List<Mensaje>> enviados(Authentication authentication) {
        List<Mensaje> mensajes = mensajeService.obtenerEnviados(authentication.getName());
        return ResponseEntity.ok(mensajes);
    }

    /**
     * Marca un mensaje recibido como leído.
     *
     * <p>Solo el receptor del mensaje puede marcarlo. Retorna 404 si el
     * mensaje no existe o no pertenece al usuario actual.</p>
     *
     * @param id ID del mensaje a marcar
     * @param authentication información del usuario autenticado
     * @return mensaje actualizado con 200 OK, o 404 si no se encontró
     */
    @PutMapping("/{id}/leer")
    public ResponseEntity<?> marcarLeido(@PathVariable String id,
                                         Authentication authentication) {
        Optional<Mensaje> resultado = mensajeService.marcarComoLeido(id, authentication.getName());
        if (resultado.isPresent()) {
            return ResponseEntity.ok(resultado.get());
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Retorna la cantidad de mensajes no leídos en la bandeja del usuario.
     *
     * @param authentication información del usuario autenticado
     * @return JSON con campo {@code count} y código 200 OK
     */
    @GetMapping("/no-leidos/count")
    public ResponseEntity<Map<String, Long>> contarNoLeidos(Authentication authentication) {
        long count = mensajeService.contarNoLeidos(authentication.getName());
        return ResponseEntity.ok(Map.of("count", count));
    }
}
