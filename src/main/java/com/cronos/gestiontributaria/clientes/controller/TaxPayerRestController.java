package com.cronos.gestiontributaria.clientes.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.cronos.gestiontributaria.common.TaxpayerType;

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
     * Listado paginado y filtrable de contribuyentes.
     * Solo accesible para GERENTE y ASESOR.
     *
     * Parámetros opcionales:
     *   q      — texto libre (busca en businessName e identificacion)
     *   tipo   — NATURAL_PERSON | LEGAL_ENTITY
     *   activo — true | false
     *   page   — número de página (default 0)
     *   size   — elementos por página (default 20)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('GERENTE', 'ASESOR')")
    public ResponseEntity<Page<TaxPayer>> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) TaxpayerType tipo,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("businessName").ascending());
        Page<TaxPayer> resultado =
                taxPayerService.findByFilters(q, tipo, activo, pageable);
        return ResponseEntity.ok(resultado);
    }

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

    /**
     * Sube un documento asociado al contribuyente autenticado.
     *
     * <p>{@code obligationId} es opcional: si viene, debe corresponder a una
     * obligación que pertenece al propio contribuyente, en caso contrario
     * se devuelve 403. El tamaño y el tipo MIME se validan en el servicio;
     * cualquier rechazo se traduce en 400.</p>
     */
    @PostMapping(value = "/me/documentos",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Document> subirDocumento(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String obligationId,
            @RequestParam(required = false) String description,
            Authentication authentication) {

        String taxPayerId = resolverTaxPayerId(authentication);

        // Si viene obligationId, verificar que pertenece al contribuyente
        // autenticado ANTES de tocar el archivo.
        if (obligationId != null && !obligationId.isBlank()) {
            boolean esPropia = taxObligationService
                    .findByTaxPayerId(taxPayerId)
                    .stream()
                    .anyMatch(ob -> obligationId.equals(ob.id()));

            if (!esPropia) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "La obligación indicada no pertenece a tu cuenta.");
            }
        }

        try {
            Document doc = documentService.uploadDocument(
                    taxPayerId,
                    (obligationId != null && !obligationId.isBlank())
                            ? obligationId : null,
                    file,
                    description);
            return ResponseEntity.status(HttpStatus.CREATED).body(doc);

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    e.getMessage());
        }
    }

    /**
     * Invierte el estado activo/inactivo de un contribuyente.
     * Solo accesible para GERENTE.
     */
    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasRole('GERENTE')")
    public ResponseEntity<TaxPayer> toggleActive(@PathVariable String id) {
        try {
            TaxPayer updated = taxPayerService.toggleActive(id);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    e.getMessage());
        }
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
