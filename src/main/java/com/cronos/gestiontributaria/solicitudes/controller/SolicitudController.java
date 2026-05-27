package com.cronos.gestiontributaria.solicitudes.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cronos.gestiontributaria.solicitudes.dto.RadicarSolicitudRequest;
import com.cronos.gestiontributaria.solicitudes.model.Solicitud;
import com.cronos.gestiontributaria.solicitudes.service.SolicitudService;

import jakarta.validation.Valid;

/**
 * Controlador REST para el sistema de solicitudes con flujo de estados.
 */
@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    private final SolicitudService solicitudService;

    public SolicitudController(SolicitudService solicitudService) {
        this.solicitudService = solicitudService;
    }

    private String currentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public Solicitud radicar(@Valid @RequestBody RadicarSolicitudRequest request) {
        return solicitudService.radicar(currentUserEmail(), request);
    }

    @GetMapping("/mis-solicitudes")
    @PreAuthorize("isAuthenticated()")
    public List<Solicitud> getMisSolicitudes() {
        return solicitudService.getMisSolicitudes(currentUserEmail());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Solicitud> getTodasLasSolicitudes() {
        return solicitudService.getTodasLasSolicitudes();
    }

    @PutMapping("/{id}/aprobar")
    @PreAuthorize("hasRole('ADMIN')")
    public Solicitud aprobar(@PathVariable String id,
                             @RequestParam String observacion) {
        return solicitudService.aprobar(id, observacion);
    }

    @PutMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('ADMIN')")
    public Solicitud rechazar(@PathVariable String id,
                              @RequestParam String observacion) {
        return solicitudService.rechazar(id, observacion);
    }
}
