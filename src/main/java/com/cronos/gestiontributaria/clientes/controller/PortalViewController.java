package com.cronos.gestiontributaria.clientes.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.cronos.gestiontributaria.auth.service.UserService;
import com.cronos.gestiontributaria.clientes.service.TaxPayerService;
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
     * Placeholder para {@code /portal/inicio} — se implementa en D-2.
     */
    @GetMapping("/inicio")
    public String inicio(Authentication authentication, Model model) {
        return "portal/inicio";
    }

    /**
     * Placeholder para {@code /portal/obligaciones} — se implementa en D-3.
     */
    @GetMapping("/obligaciones")
    public String obligaciones(Authentication authentication, Model model) {
        return "portal/obligaciones";
    }

    /**
     * Placeholder para {@code /portal/documentos} — se implementa en D-6.
     */
    @GetMapping("/documentos")
    public String documentos(Authentication authentication, Model model) {
        return "portal/documentos";
    }
}
