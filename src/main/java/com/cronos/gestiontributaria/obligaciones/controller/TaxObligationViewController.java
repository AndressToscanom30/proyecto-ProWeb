package com.cronos.gestiontributaria.obligaciones.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.cronos.gestiontributaria.auth.model.User;
import com.cronos.gestiontributaria.auth.service.UserService;
import com.cronos.gestiontributaria.clientes.model.TaxPayer;
import com.cronos.gestiontributaria.clientes.service.TaxPayerService;
import com.cronos.gestiontributaria.common.EmployeeRole;
import com.cronos.gestiontributaria.common.TaxObligationStatus;
import com.cronos.gestiontributaria.common.TaxObligationType;
import com.cronos.gestiontributaria.obligaciones.dto.CreateTaxObligationDTO;
import com.cronos.gestiontributaria.obligaciones.dto.TaxObligationResponseDTO;
import com.cronos.gestiontributaria.obligaciones.model.Document;
import com.cronos.gestiontributaria.obligaciones.service.DocumentService;
import com.cronos.gestiontributaria.obligaciones.service.TaxObligationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequestMapping("/obligaciones")
/**
 * Documentación de la entidad TaxObligationViewController.
 */
public class TaxObligationViewController {

    private final TaxObligationService obligationService;
    private final TaxPayerService taxPayerService;
    private final UserService userService;
    private final DocumentService documentService;
    private final com.cronos.gestiontributaria.common.service.EmailService emailService;

    @Value("${cronos.storage.upload-dir}")
    private String uploadDir;

    public TaxObligationViewController(TaxObligationService obligationService,
            TaxPayerService taxPayerService,
            UserService userService,
            DocumentService documentService,
            com.cronos.gestiontributaria.common.service.EmailService emailService) {
        this.obligationService = obligationService;
        this.taxPayerService = taxPayerService;
        this.userService = userService;
        this.documentService = documentService;
        this.emailService = emailService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String taxPayerId, Model model, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        model.addAttribute("usuario", user);
        model.addAttribute("contribuyentes", taxPayerService.findAll());
        model.addAttribute("selectedTaxPayerId", taxPayerId);

        if (taxPayerId != null && !taxPayerId.isBlank()) {
            TaxPayer taxPayer = taxPayerService.findById(taxPayerId);
            model.addAttribute("contribuyente", taxPayer);
            model.addAttribute("obligaciones", obligationService.findByTaxPayerId(taxPayerId));
        } else {
            model.addAttribute("contribuyente", null);
            model.addAttribute("obligaciones", obligationService.findAll());
        }
        return "obligaciones/list";
    }

    @GetMapping("/nuevo")
    public String createForm(@RequestParam(required = false) String taxPayerId, Model model, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        
        model.addAttribute("usuario", user);
        model.addAttribute("tipos", TaxObligationType.values());
        model.addAttribute("contadores", userService.findByRoleName(roleName(EmployeeRole.CONTADOR)));
        model.addAttribute("auxiliares", userService.findByRoleName(roleName(EmployeeRole.AUXILIAR_CONTADOR)));
        
        if (taxPayerId != null && !taxPayerId.isBlank()) {
            TaxPayer taxPayer = taxPayerService.findById(taxPayerId);
            model.addAttribute("contribuyente", taxPayer);
            model.addAttribute("dto", new CreateTaxObligationDTO(taxPayerId, null, null, 2026, null, null, null, null, null, null));
        } else {
            model.addAttribute("contribuyente", null);
            model.addAttribute("contribuyentes", taxPayerService.findAll());
            model.addAttribute("dto", new CreateTaxObligationDTO(null, null, null, 2026, null, null, null, null, null, null));
        }
        return "obligaciones/form";
    }

    @PostMapping("/guardar")
    public String save(@RequestParam String taxPayerId,
            @RequestParam TaxObligationType type,
            @RequestParam String fiscalPeriod,
            @RequestParam(required = false, defaultValue = "2026") int taxYear,
            @RequestParam(required = false) String notes,
            @RequestParam String counterResponsibleId,
            @RequestParam(required = false) String dueDateOverride,
            @RequestParam(required = false) String dueDateOverrideReason,
            Model model, Authentication authentication) {
        java.time.LocalDate overrideDate = null;
        try {
            if (dueDateOverride != null && !dueDateOverride.isBlank()) {
                overrideDate = java.time.LocalDate.parse(dueDateOverride);
            }
            CreateTaxObligationDTO dto = new CreateTaxObligationDTO(
                    taxPayerId, type, fiscalPeriod, taxYear, notes, overrideDate, dueDateOverrideReason,
                    counterResponsibleId, null, null);
            obligationService.create(dto);
            return "redirect:/obligaciones?taxPayerId=" + taxPayerId;
        } catch (IllegalArgumentException | java.util.NoSuchElementException e) {
            User user = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
            TaxPayer taxPayer = taxPayerService.findById(taxPayerId);
            model.addAttribute("usuario", user);
            model.addAttribute("contribuyente", taxPayer);
            model.addAttribute("tipos", TaxObligationType.values());
                model.addAttribute("contadores", userService.findByRoleName(roleName(EmployeeRole.CONTADOR)));
                model.addAttribute("auxiliares", userService.findByRoleName(roleName(EmployeeRole.AUXILIAR_CONTADOR)));
                model.addAttribute("dto", new CreateTaxObligationDTO(taxPayerId, type, fiscalPeriod, taxYear, notes,
                    overrideDate, dueDateOverrideReason, counterResponsibleId, null, null));
            model.addAttribute("error", e.getMessage());
            return "obligaciones/form";
        }
    }

    @GetMapping("/editar/{id}")
    public String editForm(@PathVariable String id, @RequestParam String taxPayerId, Model model, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        TaxPayer taxPayer = taxPayerService.findById(taxPayerId);
        TaxObligationResponseDTO obligacion = obligationService.findById(id);
        
        CreateTaxObligationDTO dto = new CreateTaxObligationDTO(
                obligacion.taxPayerId(), obligacion.type(), obligacion.fiscalPeriod(), 
                obligacion.taxYear(), obligacion.notes(), 
                obligacion.dueDateOverridden() ? obligacion.dueDate() : null, 
            obligacion.dueDateOverrideReason(),
            obligacion.counterResponsible() != null ? obligacion.counterResponsible().userId() : null,
            obligacion.dueDate(),
            obligacion.dueDateOverridden());

        model.addAttribute("usuario", user);
        model.addAttribute("contribuyente", taxPayer);
        model.addAttribute("tipos", TaxObligationType.values());
        model.addAttribute("contadores", userService.findByRoleName(roleName(EmployeeRole.CONTADOR)));
        model.addAttribute("auxiliares", userService.findByRoleName(roleName(EmployeeRole.AUXILIAR_CONTADOR)));
        model.addAttribute("currentDueDate", obligacion.dueDate());
        model.addAttribute("currentDueDateOverridden", obligacion.dueDateOverridden());
        model.addAttribute("dto", dto);
        model.addAttribute("editId", id);
        return "obligaciones/form";
    }

    @PostMapping("/actualizar/{id}")
    public String update(@PathVariable String id,
            @RequestParam String taxPayerId,
            @RequestParam TaxObligationType type,
            @RequestParam String fiscalPeriod,
            @RequestParam(required = false, defaultValue = "2026") int taxYear,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) String dueDateOverride,
            @RequestParam(required = false) String dueDateOverrideReason,
            Model model, Authentication authentication) {
        java.time.LocalDate overrideDate = null;
        try {
            if (dueDateOverride != null && !dueDateOverride.isBlank()) {
                overrideDate = java.time.LocalDate.parse(dueDateOverride);
            }
            TaxObligationResponseDTO original = obligationService.findById(id);
            CreateTaxObligationDTO dto = new CreateTaxObligationDTO(
                taxPayerId, type, fiscalPeriod, taxYear, notes, overrideDate, dueDateOverrideReason,
                original.counterResponsible() != null ? original.counterResponsible().userId() : null,
                original.dueDate(),
                original.dueDateOverridden());
            obligationService.update(id, dto);
            return "redirect:/obligaciones?taxPayerId=" + taxPayerId;
        } catch (IllegalArgumentException | java.util.NoSuchElementException e) {
            User user = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
            TaxPayer taxPayer = taxPayerService.findById(taxPayerId);
            model.addAttribute("usuario", user);
            model.addAttribute("contribuyente", taxPayer);
            model.addAttribute("tipos", TaxObligationType.values());
            model.addAttribute("contadores", userService.findByRoleName(roleName(EmployeeRole.CONTADOR)));
            model.addAttribute("auxiliares", userService.findByRoleName(roleName(EmployeeRole.AUXILIAR_CONTADOR)));
            model.addAttribute("dto", new CreateTaxObligationDTO(taxPayerId, type, fiscalPeriod, taxYear, notes,
                overrideDate, dueDateOverrideReason, null, null, null));
            model.addAttribute("editId", id);
            model.addAttribute("error", e.getMessage());
            return "obligaciones/form";
        }
    }

    @GetMapping("/eliminar/{id}")
    public String delete(@PathVariable String id, @RequestParam String taxPayerId) {
        obligationService.delete(id);
        return "redirect:/obligaciones?taxPayerId=" + taxPayerId;
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable String id,
            @RequestParam String newStatus,
            @RequestParam String taxPayerId) {
        obligationService.changeStatus(id, TaxObligationStatus.valueOf(newStatus));
        return "redirect:/obligaciones?taxPayerId=" + taxPayerId;
    }

    @PostMapping("/{id}/contador")
    public String assignCounter(@PathVariable String id,
            @RequestParam String counterResponsibleId,
            @RequestParam(required = false) String taxPayerId) {
        obligationService.assignCounter(id, counterResponsibleId);
        return taxPayerId != null && !taxPayerId.isBlank()
                ? "redirect:/obligaciones/" + id
                : "redirect:/obligaciones/" + id;
    }

    @PostMapping("/{id}/auxiliar")
    public String assignAuxiliary(@PathVariable String id,
            @RequestParam(required = false) String auxiliaryResponsibleId) {
        obligationService.assignAuxiliary(id, auxiliaryResponsibleId);
        return "redirect:/obligaciones/" + id;
    }

    @PostMapping("/{id}/requirements")
    public String addRequirement(@PathVariable String id,
            @RequestParam String requirementName,
            @RequestParam String taxPayerId) {
        obligationService.addRequirement(id, requirementName);
        // Redirige al detalle de la obligación para que el contador vea el cambio
        return "redirect:/obligaciones/" + id;
    }

    @GetMapping("/{id}")
    public String details(@PathVariable String id, Model model, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        model.addAttribute("usuario", user);
        try {
            TaxObligationResponseDTO dto = obligationService.findById(id);
            model.addAttribute("obligacion", dto);
            model.addAttribute("contadores", userService.findByRoleName(roleName(EmployeeRole.CONTADOR)));
            model.addAttribute("auxiliares", userService.findByRoleName(roleName(EmployeeRole.AUXILIAR_CONTADOR)));
            return "obligaciones/detail";
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Obligación no encontrada");
        }
    }

    @PostMapping("/{id}/notificar")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('GERENTE', 'ASESOR', 'ADMIN')")
    public String notifySingle(@PathVariable String id, @RequestParam(required = false) String taxPayerId, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttrs) {
        TaxObligationResponseDTO obligacion = obligationService.findById(id);
        int sent = sendNotificationEmailsForObligation(obligacion);
        if (sent > 0) {
            redirectAttrs.addFlashAttribute("successMessage", "Se envió notificación a " + sent + " responsable(s) de esta obligación.");
        } else {
            redirectAttrs.addFlashAttribute("warningMessage", "No hay responsables con correo asignado para notificar en esta obligación.");
        }
        return "redirect:/obligaciones?taxPayerId=" + (taxPayerId != null ? taxPayerId : "");
    }

    @PostMapping("/notificar-masivo")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('GERENTE', 'ASESOR', 'ADMIN')")
    public String notifyMassive(@RequestParam(required = false) String taxPayerId, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttrs) {
        List<TaxObligationResponseDTO> obligaciones;
        if (taxPayerId != null && !taxPayerId.isBlank()) {
            obligaciones = obligationService.findByTaxPayerId(taxPayerId);
        } else {
            obligaciones = obligationService.findAll();
        }

        int totalSent = 0;
        for (TaxObligationResponseDTO o : obligaciones) {
            totalSent += sendNotificationEmailsForObligation(o);
        }

        if (totalSent > 0) {
            redirectAttrs.addFlashAttribute("successMessage", "Se enviaron " + totalSent + " notificaciones de forma masiva.");
        } else {
            redirectAttrs.addFlashAttribute("warningMessage", "No se encontraron responsables asignados para notificar en las obligaciones actuales.");
        }
        return "redirect:/obligaciones?taxPayerId=" + (taxPayerId != null ? taxPayerId : "");
    }

    private int sendNotificationEmailsForObligation(TaxObligationResponseDTO obligacion) {
        int count = 0;
        if (obligacion.counterResponsible() != null && obligacion.counterResponsible().userEmail() != null && !obligacion.counterResponsible().userEmail().isBlank()) {
            emailService.sendObligationReminder(obligacion.counterResponsible().userEmail(), "Contador", obligacion);
            count++;
        }
        if (obligacion.auxiliaryResponsible() != null && obligacion.auxiliaryResponsible().userEmail() != null && !obligacion.auxiliaryResponsible().userEmail().isBlank()) {
            emailService.sendObligationReminder(obligacion.auxiliaryResponsible().userEmail(), "Auxiliar Contable", obligacion);
            count++;
        }
        return count;
    }

    @GetMapping("/documentos/{id}/descargar")
    public ResponseEntity<Resource> descargarDocumentoAdmin(@PathVariable String id) {
        Document documento;
        try {
            documento = documentService.findById(id);
        } catch (java.util.NoSuchElementException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento no encontrado.");
        }

        Path baseDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path archivo = baseDir.resolve(documento.getStoredFileName()).normalize();

        if (!archivo.startsWith(baseDir)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ruta de archivo no permitida.");
        }

        if (!Files.exists(archivo) || !Files.isReadable(archivo)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El archivo ya no está disponible.");
        }

        MediaType contentType = MediaType.APPLICATION_OCTET_STREAM;
        if (documento.getFileType() != null && !documento.getFileType().isBlank()) {
            try {
                contentType = MediaType.parseMediaType(documento.getFileType());
            } catch (IllegalArgumentException ignored) {
                contentType = MediaType.APPLICATION_OCTET_STREAM;
            }
        }

        String fileName = documento.getFileName() != null && !documento.getFileName().isBlank() ? documento.getFileName() : documento.getStoredFileName();
        Resource resource = new FileSystemResource(archivo);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }

    private static String roleName(EmployeeRole role) {
        return "ROLE_" + role.name();
    }
}
