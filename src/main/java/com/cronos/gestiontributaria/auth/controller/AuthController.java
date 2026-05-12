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
import com.cronos.gestiontributaria.clientes.model.TaxPayer;
import com.cronos.gestiontributaria.clientes.service.TaxPayerService;
import com.cronos.gestiontributaria.common.view.DashboardSummary;
import com.cronos.gestiontributaria.common.view.PageViewModels;
import com.cronos.gestiontributaria.common.view.PageViewModels.CalendarDayDetail;
import com.cronos.gestiontributaria.common.view.PageViewModels.MonthNav;
import com.cronos.gestiontributaria.common.view.PageViewModels.TasksPageData;
import com.cronos.gestiontributaria.common.view.PageViewModels.ReportsPageData;
import com.cronos.gestiontributaria.empleados.service.EmployeeService;
import com.cronos.gestiontributaria.obligaciones.model.TaxObligation;
import com.cronos.gestiontributaria.obligaciones.repository.TaxObligationRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        populateWorkspaceModel(model, authentication);
        return "dashboard";
    }

    /**
     * Muestra una vista dedicada del calendario fiscal.
     */
    @GetMapping("/calendario-fiscal")
    public String calendarPage(Model model, Authentication authentication) {
        populateWorkspaceModel(model, authentication);

        List<TaxObligation> obligations = obligationRepository.findAll();
        List<TaxPayer> payers = taxPayerService.findAll();
        Map<String, TaxPayer> clientsById = payers.stream()
            .filter(p -> p != null && p.getId() != null)
            .collect(Collectors.toMap(TaxPayer::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);

        List<CalendarDayDetail> dayDetails = PageViewModels.buildCalendarDayDetails(obligations, clientsById, today, currentMonth);
        MonthNav prevMonth = new MonthNav(formatMonthLabel(currentMonth.minusMonths(1)), currentMonth.minusMonths(1).getYear(), currentMonth.minusMonths(1).getMonthValue());
        MonthNav nextMonth = new MonthNav(formatMonthLabel(currentMonth.plusMonths(1)), currentMonth.plusMonths(1).getYear(), currentMonth.plusMonths(1).getMonthValue());

        model.addAttribute("calendarDays", dayDetails);
        model.addAttribute("prevMonth", prevMonth);
        model.addAttribute("nextMonth", nextMonth);
        model.addAttribute("currentMonthLabel", formatMonthLabel(currentMonth));

        return "calendario/index";
    }

    @GetMapping("/tareas")
    public String tasksPage(Model model, Authentication authentication) {
        populateWorkspaceModel(model, authentication);

        List<TaxObligation> obligations = obligationRepository.findAll();
        List<TaxPayer> payers = taxPayerService.findAll();
        Map<String, TaxPayer> clientsById = payers.stream()
            .filter(p -> p != null && p.getId() != null)
            .collect(Collectors.toMap(TaxPayer::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        LocalDate today = LocalDate.now();

        TasksPageData tasksData = PageViewModels.buildTasksPageData(obligations, clientsById, today);
        model.addAttribute("tasksData", tasksData);

        return "tareas/index";
    }

    @GetMapping("/reportes")
    public String reportsPage(Model model, Authentication authentication) {
        populateWorkspaceModel(model, authentication);

        List<TaxObligation> obligations = obligationRepository.findAll();
        LocalDate today = LocalDate.now();

        ReportsPageData reportsData = PageViewModels.buildReportsPageData(obligations, today);
        model.addAttribute("reportsData", reportsData);

        return "reportes/index";
    }

    @GetMapping("/configuracion")
    public String configurationPage(Model model, Authentication authentication) {
        populateWorkspaceModel(model, authentication);
        model.addAttribute("usuarios", userService.findAll());
        return "configuracion/index";
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
        populateWorkspaceModel(model, authentication);
        model.addAttribute("usuarios", userService.findAll());
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

    private User populateWorkspaceModel(Model model, Authentication authentication) {
        User user = loadCurrentUser(authentication);
        model.addAttribute("usuario", user);
        model.addAttribute("roles", authentication.getAuthorities());
        model.addAttribute("dashboard", DashboardSummary.build(taxPayerService.findAll(), obligationRepository.findAll()));
        model.addAttribute("totalContribuyentes", taxPayerService.count());
        model.addAttribute("totalObligaciones", obligationRepository.count());
        model.addAttribute("totalEmpleados", employeeService.count());
        model.addAttribute("totalUsuarios", userService.findAll().size());
        return user;
    }

    private User loadCurrentUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario autenticado no encontrado"));
    }

    private String formatMonthLabel(YearMonth month) {
        Locale locale = Locale.forLanguageTag("es-CO");
        String name = month.getMonth().getDisplayName(TextStyle.FULL, locale);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1) + " " + month.getYear();
    }
}