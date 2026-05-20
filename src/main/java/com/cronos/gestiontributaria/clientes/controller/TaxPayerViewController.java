package com.cronos.gestiontributaria.clientes.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.cronos.gestiontributaria.auth.model.User;
import com.cronos.gestiontributaria.auth.service.UserService;
import com.cronos.gestiontributaria.clientes.model.TaxPayer;
import com.cronos.gestiontributaria.clientes.service.TaxPayerService;
import com.cronos.gestiontributaria.common.TaxpayerType;

import jakarta.validation.Valid;

import com.cronos.gestiontributaria.obligaciones.repository.TaxObligationRepository;
import com.cronos.gestiontributaria.obligaciones.model.TaxObligation;
import java.util.List;

@Controller
@RequestMapping("/clientes")
/**
 * Documentación de la entidad TaxPayerViewController.
 */
public class TaxPayerViewController {

    private final TaxPayerService taxPayerService;
    private final UserService userService;
    private final TaxObligationRepository obligationRepository;

    public TaxPayerViewController(TaxPayerService taxPayerService, UserService userService, TaxObligationRepository obligationRepository) {
        this.taxPayerService = taxPayerService;
        this.userService = userService;
        this.obligationRepository = obligationRepository;
    }

    @GetMapping
    public String list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) TaxpayerType tipo,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model,
            Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        List<String> allowedClientIds = null;
        boolean isContador = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CONTADOR"));
        boolean isAuxiliar = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_AUXILIAR_CONTADOR"));
        
        if (isContador) {
            allowedClientIds = obligationRepository.findByCounterResponsibleUserId(user.getId())
                .stream().map(TaxObligation::getTaxPayerId).distinct().toList();
        } else if (isAuxiliar) {
            allowedClientIds = obligationRepository.findByAuxiliaryResponsibleUserId(user.getId())
                .stream().map(TaxObligation::getTaxPayerId).distinct().toList();
        }

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("businessName").ascending());
        Page<TaxPayer> resultado =
                taxPayerService.findByFilters(q, tipo, activo, allowedClientIds, pageable);

        model.addAttribute("usuario", user);
        // Alias para no romper la vista actual que itera sobre "contribuyentes".
        model.addAttribute("contribuyentes", resultado.getContent());
        model.addAttribute("clientes", resultado.getContent());
        model.addAttribute("paginaActual", page);
        model.addAttribute("totalPaginas", resultado.getTotalPages());
        model.addAttribute("totalElementos", resultado.getTotalElements());
        model.addAttribute("size", size);
        model.addAttribute("q", q);
        model.addAttribute("tipo", tipo);
        model.addAttribute("activo", activo);
        model.addAttribute("tiposTaxpayer", TaxpayerType.values());
        return "clientes/list";
    }

    @PreAuthorize("hasRole('GERENTE')")
    @GetMapping("/nuevo")
    public String createForm(Model model, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        model.addAttribute("usuario", user);
        model.addAttribute("contribuyente", new TaxPayer());
        return "clientes/form";
    }

    @PreAuthorize("hasRole('GERENTE')")
    @PostMapping("/guardar")
    public String save(@Valid @ModelAttribute("contribuyente") TaxPayer taxPayer,
                        BindingResult result, Model model, Authentication authentication) {
        if (result.hasErrors()) {
            User user = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
            model.addAttribute("usuario", user);
            return "clientes/form";
        }

        try {
            if (taxPayer.getId() != null && !taxPayer.getId().isBlank()) {
                taxPayerService.update(taxPayer.getId(), taxPayer);
            } else {
                taxPayerService.create(taxPayer);
            }
            return "redirect:/clientes";
        } catch (IllegalArgumentException e) {
            User user = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
            model.addAttribute("usuario", user);
            result.rejectValue("identificacion", "error.identificacion", e.getMessage());
            return "clientes/form";
        }
    }

    @PreAuthorize("hasRole('GERENTE')")
    @GetMapping("/editar/{id}")
    public String editForm(@PathVariable String id, Model model, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        model.addAttribute("usuario", user);
        model.addAttribute("contribuyente", taxPayerService.findById(id));
        return "clientes/form";
    }

    @PreAuthorize("hasRole('GERENTE')")
    @GetMapping("/eliminar/{id}")
    public String delete(@PathVariable String id) {
        taxPayerService.delete(id);
        return "redirect:/clientes";
    }
}
