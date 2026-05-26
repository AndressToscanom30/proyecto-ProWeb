package com.cronos.gestiontributaria.solicitudes.controller;

import java.util.List;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cronos.gestiontributaria.solicitudes.dto.SolicitudRequest;
import com.cronos.gestiontributaria.solicitudes.model.Solicitud;
import com.cronos.gestiontributaria.solicitudes.service.SolicitudService;

import jakarta.validation.Valid;

/**
 * Controlador REST para el módulo de solicitudes con flujo de estados.
 *
 * <p>Expone endpoints para radicar solicitudes (cualquier usuario autenticado)
 * y para aprobar/rechazar (solo administradores).</p>
 */
@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    private final SolicitudService solicitudService;

    public SolicitudController(SolicitudService solicitudService) {
        this.solicitudService = solicitudService;
    }

    /**
     * Radica una nueva solicitud con estado PENDIENTE.
     *
     * @param request datos de la solicitud (tipo y descripción)
     * @param authentication información del usuario autenticado
     * @return solicitud creada con código 201 Created
     */
    @PostMapping
    public ResponseEntity<Solicitud> crearSolicitud(@Valid @RequestBody SolicitudRequest request,
                                                     Authentication authentication) {
        Solicitud solicitud = solicitudService.crearSolicitud(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(solicitud);
    }

    /**
     * Lista las solicitudes del usuario autenticado.
     *
     * @param authentication información del usuario autenticado
     * @return lista de solicitudes del usuario con código 200 OK
     */
    @GetMapping("/mis-solicitudes")
    public ResponseEntity<List<Solicitud>> misSolicitudes(Authentication authentication) {
        List<Solicitud> solicitudes = solicitudService.listarPorUsuario(authentication.getName());
        return ResponseEntity.ok(solicitudes);
    }

    /**
     * Lista todas las solicitudes del sistema (solo ADMIN).
     *
     * <p>La restricción de acceso se configura en {@code SecurityConfig}
     * mediante {@code .hasRole("ADMIN")} para {@code GET /api/solicitudes}.</p>
     *
     * @return lista completa de solicitudes con código 200 OK
     */
    @GetMapping
    public ResponseEntity<List<Solicitud>> listarTodas() {
        List<Solicitud> solicitudes = solicitudService.listarTodas();
        return ResponseEntity.ok(solicitudes);
    }

    /**
     * Aprueba una solicitud registrando la observación del administrador.
     *
     * <p>La restricción de acceso se configura en {@code SecurityConfig}.</p>
     *
     * @param id ID de la solicitud a aprobar
     * @param observacion texto de observación del administrador
     * @return solicitud actualizada con 200 OK, o 404 si no existe
     */
    @PutMapping("/{id}/aprobar")
    public ResponseEntity<Solicitud> aprobar(@PathVariable String id,
                                              @RequestParam String observacion) {
        Optional<Solicitud> resultado = solicitudService.aprobar(id, observacion);
        return resultado.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Rechaza una solicitud registrando la observación del administrador.
     *
     * <p>La restricción de acceso se configura en {@code SecurityConfig}.</p>
     *
     * @param id ID de la solicitud a rechazar
     * @param observacion texto de observación del administrador
     * @return solicitud actualizada con 200 OK, o 404 si no existe
     */
    @PutMapping("/{id}/rechazar")
    public ResponseEntity<Solicitud> rechazar(@PathVariable String id,
                                               @RequestParam String observacion) {
        Optional<Solicitud> resultado = solicitudService.rechazar(id, observacion);
        return resultado.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
