package com.cronos.gestiontributaria.obligaciones.controller;

import org.springframework.http.HttpStatus;
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
import com.cronos.gestiontributaria.obligaciones.dto.TaxObligationResponseDTO;
import com.cronos.gestiontributaria.obligaciones.service.TaxObligationService;

import java.util.List;

@Controller
@RequestMapping("/mis-obligaciones")
@PreAuthorize("hasAnyRole('CONTADOR', 'AUXILIAR_CONTADOR')")
public class MyObligationsViewController {

    private final TaxObligationService taxObligationService;
    private final UserService userService;

    public MyObligationsViewController(TaxObligationService taxObligationService, UserService userService) {
        this.taxObligationService = taxObligationService;
        this.userService = userService;
    }

    @GetMapping
    public String listMyObligations(Model model, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        
        boolean isContador = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CONTADOR"));
        
        List<TaxObligationResponseDTO> obligaciones;
        if (isContador) {
            obligaciones = taxObligationService.findByCounterResponsibleUserId(user.getId());
        } else {
            obligaciones = taxObligationService.findByAuxiliaryResponsibleUserId(user.getId());
        }
        
        model.addAttribute("usuario", user);
        model.addAttribute("obligaciones", obligaciones);
        return "obligaciones/mis-obligaciones";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable String id, Model model, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        
        TaxObligationResponseDTO obligacion = taxObligationService.findById(id);
        
        // Medida de seguridad: Validar que el usuario autenticado es responsable de la obligación
        boolean isContador = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CONTADOR"));
        
        boolean hasAccess = false;
        if (isContador) {
            if (obligacion.counterResponsible() != null && obligacion.counterResponsible().userId().equals(user.getId())) {
                hasAccess = true;
            }
        } else {
            if (obligacion.auxiliaryResponsible() != null && obligacion.auxiliaryResponsible().userId().equals(user.getId())) {
                hasAccess = true;
            }
        }
        
        if (!hasAccess) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a esta obligación, no eres el responsable.");
        }
        
        model.addAttribute("usuario", user);
        model.addAttribute("obligacion", obligacion);
        return "obligaciones/detail";
    }
}
