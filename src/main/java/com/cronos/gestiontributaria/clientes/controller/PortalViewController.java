package com.cronos.gestiontributaria.clientes.controller;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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
 * Controlador MVC del portal del contribuyente.
 *
 * <p>Maneja las rutas {@code /portal/**} para el área privada del
 * contribuyente autenticado.</p>
 */
@Controller
@RequestMapping("/portal")
@PreAuthorize("hasRole('CONTRIBUYENTE')")
public class PortalViewController {

    private static final DateTimeFormatter DOCUMENT_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String OBLIGACION_NO_DISPONIBLE = "Obligación no disponible";

    @Value("${cronos.storage.upload-dir}")
    private String uploadDir;

    private final UserService userService;

    private final TaxPayerService taxPayerService;

    private final TaxObligationService taxObligationService;

    private final DocumentService documentService;

    public PortalViewController(UserService userService,
                                TaxPayerService taxPayerService,
                                TaxObligationService taxObligationService,
                                DocumentService documentService) {
        this.userService = userService;
        this.taxPayerService = taxPayerService;
        this.taxObligationService = taxObligationService;
        this.documentService = documentService;
    }

    /**
     * Redirige la raíz del portal a la vista de inicio.
     */
    @GetMapping
    public String index() {
        return "redirect:/portal/inicio";
    }

    /**
     * Vista de inicio del portal: muestra los datos de perfil del
     * contribuyente autenticado en modo solo-lectura.
     */
    @GetMapping("/inicio")
    public String inicio(Authentication authentication, Model model) {
        String taxPayerId = resolverTaxPayerId(authentication);
        TaxPayer taxPayer = taxPayerService.findById(taxPayerId);
        model.addAttribute("contribuyente", taxPayer);
        return "portal/inicio";
    }

    /**
     * Listado de las obligaciones tributarias del contribuyente
     * autenticado.
     */
    @GetMapping("/obligaciones")
    public String obligaciones(Authentication authentication, Model model) {
        String taxPayerId = resolverTaxPayerId(authentication);
        List<TaxObligationResponseDTO> obligaciones =
                taxObligationService.findByTaxPayerId(taxPayerId);
        model.addAttribute("obligaciones", obligaciones);
        return "portal/obligaciones";
    }

    /**
     * Detalle de una obligación tributaria.
     *
     * <p>El guard se hace filtrando la lista del contribuyente autenticado:
     * si la obligación pedida no aparece en {@code findByTaxPayerId(taxPayerId)},
     * devolvemos 403. No exponemos {@code findById} directamente para evitar
     * que un id ajeno pueda leerse "por accidente".</p>
     */
    @GetMapping("/obligaciones/{id}")
    public String detalleObligacion(@PathVariable String id,
                                    Authentication authentication,
                                    Model model) {
        String taxPayerId = resolverTaxPayerId(authentication);

        TaxObligationResponseDTO obligacion = taxObligationService
                .findByTaxPayerId(taxPayerId)
                .stream()
                .filter(ob -> id.equals(ob.id()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "No tienes acceso a esta obligación."));

        List<Document> documentos = documentService.findByObligation(id);

        model.addAttribute("obligacion", obligacion);
        model.addAttribute("documentos", documentos);
        model.addAttribute("taxPayerId", taxPayerId);
        return "portal/obligacion-detalle";
    }

    /**
     * Muestra todos los documentos del contribuyente autenticado.
     */
    @GetMapping("/documentos")
    public String documentos(Authentication authentication, Model model) {
        String taxPayerId = resolverTaxPayerId(authentication);
        List<Document> documentos = documentService.findByTaxPayer(taxPayerId);
        Map<String, String> etiquetasObligaciones = construirEtiquetasObligaciones(documentos);

        List<DocumentView> documentosVista = documentos.stream()
                .map(documento -> new DocumentView(
                        documento.getId(),
                        normalizarNombre(documento.getFileName(), documento.getStoredFileName()),
                        formatearTipoArchivo(documento.getFileType()),
                        formatearTamano(documento.getFileSize()),
                        formatearFecha(documento.getUploadedAt()),
                construirEtiquetaObligacion(documento.getObligationId(), etiquetasObligaciones)))
                .toList();

        model.addAttribute("documentos", documentosVista);
        return "portal/documentos";
    }

    /**
     * Descarga un documento del contribuyente autenticado.
     */
    @GetMapping("/documentos/{id}/descargar")
    public ResponseEntity<Resource> descargarDocumento(@PathVariable String id,
                                                       Authentication authentication) {
        String taxPayerId = resolverTaxPayerId(authentication);
        Document documento;

        try {
            documento = documentService.findById(id);
        } catch (NoSuchElementException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento no encontrado.");
        }

        if (!taxPayerId.equals(documento.getTaxPayerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No tienes acceso a este documento.");
        }

        Path baseDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path archivo = baseDir.resolve(documento.getStoredFileName()).normalize();

        if (!archivo.startsWith(baseDir)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Ruta de archivo no permitida.");
        }

        if (!Files.exists(archivo) || !Files.isReadable(archivo)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "El archivo ya no está disponible.");
        }

        MediaType contentType = MediaType.APPLICATION_OCTET_STREAM;
        if (documento.getFileType() != null && !documento.getFileType().isBlank()) {
            try {
                contentType = MediaType.parseMediaType(documento.getFileType());
            } catch (IllegalArgumentException ignored) {
                contentType = MediaType.APPLICATION_OCTET_STREAM;
            }
        }

        String nombreDescarga = normalizarNombre(documento.getFileName(), documento.getStoredFileName());
        Resource resource = new FileSystemResource(archivo);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(nombreDescarga, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }

    // ── privado ──────────────────────────────────────────────

    /**
     * Resuelve el {@code taxPayerId} del usuario autenticado.
     * Lanza 403 si el User no existe o no está vinculado a ningún
     * contribuyente.
     */
    private String resolverTaxPayerId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Usuario no autenticado.");
        }
        return userService.findByEmail(authentication.getName())
                .map(User::getTaxPayerId)
                .filter(id -> id != null && !id.isBlank())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Tu cuenta no está vinculada a ningún contribuyente."));
    }

    private Map<String, String> construirEtiquetasObligaciones(List<Document> documentos) {
        Map<String, String> etiquetas = new HashMap<>();

        for (Document documento : documentos) {
            String obligationId = documento.getObligationId();
            if (obligationId == null || obligationId.isBlank() || etiquetas.containsKey(obligationId)) {
                continue;
            }

            try {
                etiquetas.put(obligationId, construirEtiquetaObligacion(taxObligationService.findById(obligationId)));
            } catch (NoSuchElementException exception) {
                etiquetas.put(obligationId, OBLIGACION_NO_DISPONIBLE);
            }
        }

        return etiquetas;
    }

    private String construirEtiquetaObligacion(String obligationId, Map<String, String> etiquetasObligaciones) {
        if (obligationId == null || obligationId.isBlank()) {
            return "Sin vincular";
        }

        return etiquetasObligaciones.getOrDefault(obligationId, OBLIGACION_NO_DISPONIBLE);
    }

    private String construirEtiquetaObligacion(TaxObligationResponseDTO obligacion) {
        if (obligacion == null) {
            return OBLIGACION_NO_DISPONIBLE;
        }

        String tipo = "Obligación";
        if (obligacion.type() != null) {
            if (obligacion.type().getDescription() != null) {
                tipo = obligacion.type().getDescription();
            } else {
                tipo = obligacion.type().name().replace('_', ' ');
            }
        }

        if (obligacion.fiscalPeriod() == null || obligacion.fiscalPeriod().isBlank()) {
            return tipo;
        }

        return tipo + " · " + obligacion.fiscalPeriod();
    }

    private String normalizarNombre(String fileName, String storedFileName) {
        if (fileName != null && !fileName.isBlank()) {
            return fileName;
        }
        if (storedFileName != null && !storedFileName.isBlank()) {
            return storedFileName;
        }
        return "documento";
    }

    private String formatearTipoArchivo(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return "—";
        }

        return switch (mimeType) {
            case "application/pdf" -> "PDF";
            case "image/jpeg" -> "JPG";
            case "image/png" -> "PNG";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "XLSX";
            default -> mimeType;
        };
    }

    private String formatearTamano(Long bytes) {
        if (bytes == null) {
            return "—";
        }

        if (bytes < 1024) {
            return bytes + " B";
        }

        double value = bytes.doubleValue() / 1024d;
        String unit = "KB";

        if (value >= 1024d) {
            value = value / 1024d;
            unit = "MB";
        }
        if (value >= 1024d) {
            value = value / 1024d;
            unit = "GB";
        }

        return String.format(Locale.US, "%.1f %s", value, unit).replace('.', ',');
    }

    private String formatearFecha(LocalDateTime fecha) {
        if (fecha == null) {
            return "—";
        }
        return DOCUMENT_DATE_FORMAT.format(fecha);
    }

    public record DocumentView(
            String id,
            String fileName,
            String fileTypeLabel,
            String fileSizeLabel,
            String uploadedAtLabel,
            String obligationLabel) {
    }
}
