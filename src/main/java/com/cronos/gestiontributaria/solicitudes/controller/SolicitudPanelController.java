package com.cronos.gestiontributaria.solicitudes.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.cronos.gestiontributaria.auth.model.User;
import com.cronos.gestiontributaria.auth.service.UserService;
import com.cronos.gestiontributaria.solicitudes.model.EstadoSolicitud;
import com.cronos.gestiontributaria.solicitudes.model.Solicitud;
import com.cronos.gestiontributaria.solicitudes.service.SolicitudService;

/**
 * Controlador MVC para el panel visual de solicitudes (administrador).
 *
 * <p>Renderiza una página Thymeleaf con indicadores (KPIs) y una tabla
 * de todas las solicitudes del sistema. Solo accesible por usuarios
 * con rol {@code ADMIN}.</p>
 */
@Controller
public class SolicitudPanelController {

    private final SolicitudService solicitudService;
    private final UserService userService;

    public SolicitudPanelController(SolicitudService solicitudService, UserService userService) {
        this.solicitudService = solicitudService;
        this.userService = userService;
    }

    /**
     * Muestra el panel de administración de solicitudes.
     *
     * <p>Calcula en tiempo real los totales por estado y carga la lista
     * completa de solicitudes para la tabla.</p>
     *
     * @param model modelo de vista con KPIs y lista de solicitudes
     * @param authentication información del usuario autenticado
     * @return nombre de la plantilla Thymeleaf
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/solicitudes/panel")
    public String panelSolicitudes(Model model, Authentication authentication) {
        // Obtenemos el usuario autenticado para el layout de Thymeleaf
        if (authentication != null && authentication.getName() != null) {
            userService.findByEmail(authentication.getName())
                    .ifPresent(user -> model.addAttribute("usuario", user));
        } else {
            model.addAttribute("usuario", new User()); // fallback
        }

        // KPIs calculados en tiempo real desde la base de datos
        long total = solicitudService.contarTotal();
        long pendientes = solicitudService.contarPorEstado(EstadoSolicitud.PENDIENTE);
        long aprobadas = solicitudService.contarPorEstado(EstadoSolicitud.APROBADA);
        long rechazadas = solicitudService.contarPorEstado(EstadoSolicitud.RECHAZADA);

        model.addAttribute("totalSolicitudes", total);
        model.addAttribute("pendientes", pendientes);
        model.addAttribute("aprobadas", aprobadas);
        model.addAttribute("rechazadas", rechazadas);

        // Lista completa para la tabla
        List<Solicitud> solicitudes = solicitudService.listarTodas();
        model.addAttribute("solicitudes", solicitudes);

        return "admin/solicitudes/panel";
    }
}
