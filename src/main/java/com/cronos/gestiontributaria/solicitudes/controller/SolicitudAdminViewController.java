package com.cronos.gestiontributaria.solicitudes.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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

    public SolicitudAdminViewController(SolicitudService solicitudService) {
        this.solicitudService = solicitudService;
    }

    @GetMapping("/panel")
    public String panel(Model model) {
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
