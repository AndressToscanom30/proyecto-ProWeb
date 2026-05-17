package com.cronos.gestiontributaria.clientes.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
import com.cronos.gestiontributaria.clientes.model.TaxPayer;
import com.cronos.gestiontributaria.clientes.service.TaxPayerService;
import com.cronos.gestiontributaria.obligaciones.dto.TaxObligationResponseDTO;
import com.cronos.gestiontributaria.obligaciones.service.DocumentService;
import com.cronos.gestiontributaria.obligaciones.service.TaxObligationService;

/**
 * Controlador MVC del portal del contribuyente.
 *
 * <p>Maneja las rutas {@code /portal/**}. En este paso (D-1) solo declara
 * las rutas para que el layout pueda navegar sin 404; el poblado real
 * del {@code Model} llega en D-2, D-3 y D-6.</p>
 *
 * <p>El acceso a estas rutas está restringido a {@code ROLE_CONTRIBUYENTE}
 * por la regla global de {@code SecurityConfig}.</p>
 */
@Controller
@RequestMapping("/portal")
@PreAuthorize("hasRole('CONTRIBUYENTE')")
public class PortalViewController {

    @Autowired
    private UserService userService;

    @Autowired
    private TaxPayerService taxPayerService;

    @Autowired
    private TaxObligationService taxObligationService;

    @Autowired
    private DocumentService documentService;

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
     * Placeholder para {@code /portal/obligaciones/{id}} — se implementa en D-4.
     */
    @GetMapping("/obligaciones/{id}")
    public String detalleObligacion(@PathVariable String id,
                                    Authentication authentication,
                                    Model model) {
        return "portal/obligacion-detalle";
    }

    /**
     * Placeholder para {@code /portal/documentos} — se implementa en D-6.
     */
    @GetMapping("/documentos")
    public String documentos(Authentication authentication, Model model) {
        return "portal/documentos";
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
}
