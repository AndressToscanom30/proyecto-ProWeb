package com.cronos.gestiontributaria.solicitudes.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.cronos.gestiontributaria.auth.model.User;
import com.cronos.gestiontributaria.auth.service.UserService;
import com.cronos.gestiontributaria.solicitudes.model.EstadoSolicitud;
import com.cronos.gestiontributaria.solicitudes.model.Solicitud;
import com.cronos.gestiontributaria.solicitudes.service.SolicitudService;

/**
 * Controlador MVC para el panel visual de solicitudes (vista de administrador).
 */
@Controller
@RequestMapping("/admin/solicitudes")
@PreAuthorize("hasRole('ADMIN')")
public class SolicitudAdminViewController {

    private final SolicitudService solicitudService;
    private final UserService userService;

    public SolicitudAdminViewController(SolicitudService solicitudService,
                                        UserService userService) {
        this.solicitudService = solicitudService;
        this.userService = userService;
    }

    @GetMapping("/panel")
    public String panel(Model model) {
        // Inyectar usuario para el layout
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User usuario = userService.findByEmail(email).orElseThrow();
        model.addAttribute("usuario", usuario);

        // Datos del panel
        List<Solicitud> todas = solicitudService.getTodasLasSolicitudes();

        long totalGeneral    = todas.size();
        long totalPendientes = todas.stream()
                .filter(s -> s.getEstado() == EstadoSolicitud.PENDIENTE).count();
        long totalAprobadas  = todas.stream()
                .filter(s -> s.getEstado() == EstadoSolicitud.APROBADA).count();
        long totalRechazadas = todas.stream()
                .filter(s -> s.getEstado() == EstadoSolicitud.RECHAZADA).count();

        model.addAttribute("solicitudes",     todas);
        model.addAttribute("totalGeneral",    totalGeneral);
        model.addAttribute("totalPendientes", totalPendientes);
        model.addAttribute("totalAprobadas",  totalAprobadas);
        model.addAttribute("totalRechazadas", totalRechazadas);

        return "admin/solicitudes-panel";
    }
}
