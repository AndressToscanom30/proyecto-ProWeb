package com.cronos.gestiontributaria.obligaciones.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cronos.gestiontributaria.common.TaxObligationStatus;
import com.cronos.gestiontributaria.obligaciones.dto.CreateTaxObligationDTO;
import com.cronos.gestiontributaria.obligaciones.dto.TaxObligationResponseDTO;
import com.cronos.gestiontributaria.obligaciones.service.TaxObligationService;

/**
 * Controlador REST para la gestión de obligaciones tributarias.
 *
 * <p>Expone endpoints CRUD y un endpoint PATCH para cambio de estado.</p>
 */
@RestController
@RequestMapping("/api/obligaciones")
public class TaxObligationController {

    private final TaxObligationService service;

    public TaxObligationController(TaxObligationService service) {
        this.service = service;
    }

    /**
     * Crea una nueva obligación tributaria.
     */
    @PostMapping
    public ResponseEntity<TaxObligationResponseDTO> create(
            @RequestBody CreateTaxObligationDTO dto) {
        TaxObligationResponseDTO created = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Obtiene una obligación por su ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TaxObligationResponseDTO> findById(
            @PathVariable String id) {
        return ResponseEntity.ok(service.findById(id));
    }

    /**
     * Lista las obligaciones de un contribuyente.
     */
    @GetMapping
    public ResponseEntity<List<TaxObligationResponseDTO>> findByTaxPayer(
            @RequestParam String taxPayerId) {
        return ResponseEntity.ok(service.findByTaxPayerId(taxPayerId));
    }

    /**
     * Actualiza una obligación existente.
     */
    @PutMapping("/{id}")
    public ResponseEntity<TaxObligationResponseDTO> update(
            @PathVariable String id,
            @RequestBody CreateTaxObligationDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    /**
     * Elimina una obligación por su ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Cambia el estado de una obligación.
     * Body esperado: { "newStatus": "COMPLETED" }
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<TaxObligationResponseDTO> changeStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        String statusStr = body.get("newStatus");
        if (statusStr == null || statusStr.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        TaxObligationStatus newStatus = TaxObligationStatus.valueOf(statusStr);
        return ResponseEntity.ok(service.changeStatus(id, newStatus));
    }
}
