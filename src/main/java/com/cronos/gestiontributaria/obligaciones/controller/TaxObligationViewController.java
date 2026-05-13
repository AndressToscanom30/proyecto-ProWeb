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
import com.cronos.gestiontributaria.common.TaxObligationStatus;
import com.cronos.gestiontributaria.common.TaxObligationType;
import com.cronos.gestiontributaria.obligaciones.dto.CreateTaxObligationDTO;
import com.cronos.gestiontributaria.obligaciones.dto.TaxObligationResponseDTO;
import com.cronos.gestiontributaria.obligaciones.service.TaxObligationService;

@Controller
@RequestMapping("/obligaciones")
/**
 * Documentación de la entidad TaxObligationViewController.
 */
public class TaxObligationViewController {

    private final TaxObligationService obligationService;
    private final TaxPayerService taxPayerService;
    private final UserService userService;

    public TaxObligationViewController(TaxObligationService obligationService,
                                        TaxPayerService taxPayerService,
                                        UserService userService) {
        this.obligationService = obligationService;
        this.taxPayerService = taxPayerService;
        this.userService = userService;
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
    public String createForm(@RequestParam String taxPayerId, Model model, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        TaxPayer taxPayer = taxPayerService.findById(taxPayerId);
        model.addAttribute("usuario", user);
        model.addAttribute("contribuyente", taxPayer);
        model.addAttribute("tipos", TaxObligationType.values());
        model.addAttribute("dto", new CreateTaxObligationDTO(taxPayerId, null, null, 2026, null, null, null));
        return "obligaciones/form";
    }

    @PostMapping("/guardar")
    public String save(@RequestParam String taxPayerId,
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
            CreateTaxObligationDTO dto = new CreateTaxObligationDTO(
                    taxPayerId, type, fiscalPeriod, taxYear, notes, overrideDate, dueDateOverrideReason);
            obligationService.create(dto);
            return "redirect:/obligaciones?taxPayerId=" + taxPayerId;
        } catch (IllegalArgumentException | java.util.NoSuchElementException e) {
            User user = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
            TaxPayer taxPayer = taxPayerService.findById(taxPayerId);
            model.addAttribute("usuario", user);
            model.addAttribute("contribuyente", taxPayer);
            model.addAttribute("tipos", TaxObligationType.values());
            model.addAttribute("dto", new CreateTaxObligationDTO(taxPayerId, type, fiscalPeriod, taxYear, notes, overrideDate, dueDateOverrideReason));
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
    @GetMapping("/{id}")
    public String details(@PathVariable String id, Model model, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        model.addAttribute("usuario", user);
        try {
            TaxObligationResponseDTO dto = obligationService.findById(id);
            model.addAttribute("obligacion", dto);
            return "obligaciones/detail";
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Obligación no encontrada");
        }
    }
}
