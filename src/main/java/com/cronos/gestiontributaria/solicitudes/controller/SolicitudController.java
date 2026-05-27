package com.cronos.gestiontributaria.solicitudes.controller;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.cronos.gestiontributaria.solicitudes.dto.CrearSolicitudRequest;
import com.cronos.gestiontributaria.solicitudes.model.Solicitud;
import com.cronos.gestiontributaria.solicitudes.service.SolicitudService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    private final SolicitudService solicitudService;

    public SolicitudController(SolicitudService solicitudService) {
        this.solicitudService = solicitudService;
    }

    @PostMapping
    public ResponseEntity<Solicitud> radicar(
            @Valid @RequestBody CrearSolicitudRequest request,
            Authentication authentication) {
        Solicitud solicitud = solicitudService.radicar(
                authentication.getName(),
                request.getTipo(),
                request.getDescripcion());
        return ResponseEntity.status(HttpStatus.CREATED).body(solicitud);
    }

    @GetMapping("/mis-solicitudes")
    public ResponseEntity<List<Solicitud>> obtenerMisSolicitudes(Authentication authentication) {
        List<Solicitud> solicitudes = solicitudService.obtenerMisSolicitudes(authentication.getName());
        return ResponseEntity.ok(solicitudes);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Solicitud>> obtenerTodas() {
        List<Solicitud> solicitudes = solicitudService.obtenerTodas();
        return ResponseEntity.ok(solicitudes);
    }

    @PutMapping("/{id}/aprobar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Solicitud> aprobar(
            @PathVariable String id,
            @RequestParam String observacion) {
        try {
            Solicitud solicitud = solicitudService.aprobar(id, observacion);
            return ResponseEntity.ok(solicitud);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PutMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Solicitud> rechazar(
            @PathVariable String id,
            @RequestParam String observacion) {
        try {
            Solicitud solicitud = solicitudService.rechazar(id, observacion);
            return ResponseEntity.ok(solicitud);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
