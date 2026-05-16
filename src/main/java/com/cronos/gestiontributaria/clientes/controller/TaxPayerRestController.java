package com.cronos.gestiontributaria.clientes.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.cronos.gestiontributaria.auth.model.User;
import com.cronos.gestiontributaria.auth.service.UserService;
import com.cronos.gestiontributaria.clientes.model.TaxPayer;
import com.cronos.gestiontributaria.clientes.service.TaxPayerService;
import com.cronos.gestiontributaria.obligaciones.dto.TaxObligationResponseDTO;
import com.cronos.gestiontributaria.obligaciones.model.Document;
import com.cronos.gestiontributaria.obligaciones.service.DocumentService;
import com.cronos.gestiontributaria.obligaciones.service.TaxObligationService;

/**
 * API REST de solo lectura del portal del contribuyente.
 *
 * <p>El usuario autenticado solo puede acceder a sus propios datos: el
 * {@code taxPayerId} se resuelve a partir de la sesión, nunca desde un
 * parámetro de la URL. Cualquier intento de pasar un id externo o de
 * acceder sin vínculo a un contribuyente devuelve 403.</p>
 */
@RestController
@RequestMapping("/api/contribuyente")
public class TaxPayerRestController {

    @Autowired
    private TaxPayerService taxPayerService;

    @Autowired
    private TaxObligationService taxObligationService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private UserService userService;

    /**
     * Retorna los datos del contribuyente autenticado.
     */
    @GetMapping("/me")
    public ResponseEntity<TaxPayer> getMiPerfil(Authentication authentication) {
        String taxPayerId = resolverTaxPayerId(authentication);
        TaxPayer tp = taxPayerService.findById(taxPayerId);
        return ResponseEntity.ok(tp);
    }

    /**
     * Retorna las obligaciones del contribuyente autenticado.
     */
    @GetMapping("/me/obligaciones")
    public ResponseEntity<List<TaxObligationResponseDTO>> getMisObligaciones(
            Authentication authentication) {
        String taxPayerId = resolverTaxPayerId(authentication);
        List<TaxObligationResponseDTO> obligaciones =
                taxObligationService.findByTaxPayerId(taxPayerId);
        return ResponseEntity.ok(obligaciones);
    }

    /**
     * Retorna los documentos del contribuyente autenticado,
     * ordenados por fecha de subida descendente.
     */
    @GetMapping("/me/documentos")
    public ResponseEntity<List<Document>> getMisDocumentos(
            Authentication authentication) {
        String taxPayerId = resolverTaxPayerId(authentication);
        List<Document> documentos = documentService.findByTaxPayer(taxPayerId);
        return ResponseEntity.ok(documentos);
    }

    // ── privado ──────────────────────────────────────────────

    /**
     * Extrae el taxPayerId del usuario autenticado.
     *
     * @throws ResponseStatusException 403 si el User no existe o no está
     *         vinculado a ningún TaxPayer.
     */
    private String resolverTaxPayerId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Usuario no autenticado.");
        }
        String email = authentication.getName();
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Usuario no encontrado."));

        if (user.getTaxPayerId() == null || user.getTaxPayerId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Tu cuenta no está vinculada a ningún contribuyente.");
        }
        return user.getTaxPayerId();
    }
}
