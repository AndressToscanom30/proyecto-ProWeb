package com.cronos.gestiontributaria.clientes.controller;

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
import org.springframework.web.server.ResponseStatusException;

import com.cronos.gestiontributaria.auth.model.User;
import com.cronos.gestiontributaria.auth.service.UserService;
import com.cronos.gestiontributaria.clientes.model.TaxPayer;
import com.cronos.gestiontributaria.clientes.service.TaxPayerService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/clientes")
/**
 * Documentación de la entidad TaxPayerViewController.
 */
public class TaxPayerViewController {

    private final TaxPayerService taxPayerService;
    private final UserService userService;

    public TaxPayerViewController(TaxPayerService taxPayerService, UserService userService) {
        this.taxPayerService = taxPayerService;
        this.userService = userService;
    }

    @GetMapping
    public String list(Model model, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        model.addAttribute("usuario", user);
        model.addAttribute("contribuyentes", taxPayerService.findAll());
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
