package com.cronos.gestiontributaria.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import com.cronos.gestiontributaria.auth.model.User;
import com.cronos.gestiontributaria.auth.service.UserService;
import com.cronos.gestiontributaria.clientes.service.TaxPayerService;
import com.cronos.gestiontributaria.common.view.DashboardSummary;
import com.cronos.gestiontributaria.empleados.service.EmployeeService;
import com.cronos.gestiontributaria.obligaciones.repository.TaxObligationRepository;

import jakarta.validation.Valid;

/**
 * Controlador web para autenticación, registro y paneles de usuario.
 *
 * <p>Expone las rutas públicas de login y registro, el dashboard autenticado,
 * el panel de administración y la vista de acceso denegado.</p>
 */
@Controller
public class AuthController {
    private final UserService userService;
    private final TaxPayerService taxPayerService;
    private final EmployeeService employeeService;
    private final TaxObligationRepository obligationRepository;

    public AuthController(UserService userService, TaxPayerService taxPayerService,
                           EmployeeService employeeService,
                           TaxObligationRepository obligationRepository) {
        this.userService = userService;
        this.taxPayerService = taxPayerService;
        this.employeeService = employeeService;
        this.obligationRepository = obligationRepository;
    }

    /**
     * Redirige la raíz hacia el dashboard.
     *
     * @return redirección a {@code /dashboard}
     */
    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    /**
     * Muestra el formulario personalizado de login.
     *
     * @return nombre de la vista Thymeleaf del login
     */
    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    /**
     * Muestra el formulario de registro de usuarios.
     *
     * @param model modelo de vista para inyectar el formulario
     * @return nombre de la vista de registro
     */
    @GetMapping("/registro")
    public String registerForm(Model model) {
        if (!model.containsAttribute("usuario")) {
            model.addAttribute("usuario", new User());
        }
        return "auth/registro";
    }

    /**
     * Procesa el registro de un nuevo usuario.
     *
     * @param user datos enviados desde el formulario
     * @param result contenedor de errores de validación
     * @return redirección al login o la misma vista con errores
     */
    @PostMapping("/registro")
    public String register(@Valid @ModelAttribute("usuario") User user, BindingResult result) {
        if (result.hasErrors()) {
            return "auth/registro";
        }

        try {
            userService.register(user);
            return "redirect:/login?registrado";
        } catch (IllegalArgumentException exception) {
            result.rejectValue("email", "error.email", exception.getMessage());
            return "auth/registro";
        }
    }

    /**
     * Muestra el dashboard del usuario autenticado.
     *
     * @param model modelo de vista con datos del usuario
     * @param authentication información de autenticación actual
     * @return nombre de la vista dashboard
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario autenticado no encontrado"));
        model.addAttribute("usuario", user);
        model.addAttribute("roles", authentication.getAuthorities());
        model.addAttribute("dashboard", DashboardSummary.build(taxPayerService.findAll(), obligationRepository.findAll()));
        model.addAttribute("totalContribuyentes", taxPayerService.count());
        model.addAttribute("totalObligaciones", obligationRepository.count());
        model.addAttribute("totalEmpleados", employeeService.count());
        return "dashboard";
    }

    /**
     * Muestra el panel de administración restringido a {@code ADMIN}.
     *
     * @param model modelo de vista con el listado de usuarios
     * @return nombre de la vista del panel admin
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public String adminPanel(Model model, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        model.addAttribute("usuario", user);
        model.addAttribute("usuarios", userService.findAll());
        model.addAttribute("totalUsuarios", userService.findAll().size());
        return "admin/panel";
    }

    /**
     * Vista personalizada de acceso denegado.
     *
     * @return nombre de la vista 403
     */
    @RequestMapping("/403")
    public String accessDenied() {
        return "error/403";
    }
}