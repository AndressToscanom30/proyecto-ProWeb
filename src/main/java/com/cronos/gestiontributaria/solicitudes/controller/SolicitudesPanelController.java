package com.cronos.gestiontributaria.solicitudes.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.cronos.gestiontributaria.auth.model.User;
import com.cronos.gestiontributaria.auth.service.UserService;
import com.cronos.gestiontributaria.solicitudes.model.EstadoSolicitud;
import com.cronos.gestiontributaria.solicitudes.service.SolicitudService;

@Controller
@RequestMapping("/admin/solicitudes")
public class SolicitudesPanelController {

    private final SolicitudService solicitudService;
    private final UserService userService;

    public SolicitudesPanelController(SolicitudService solicitudService, UserService userService) {
        this.solicitudService = solicitudService;
        this.userService = userService;
    }

    @GetMapping("/panel")
    public String panel(Model model, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName()).orElse(null);
        model.addAttribute("usuario", user);
        model.addAttribute("roles", authentication.getAuthorities());

        long total = solicitudService.contarTotal();
        long pendientes = solicitudService.contarPorEstado(EstadoSolicitud.PENDIENTE);
        long aprobadas = solicitudService.contarPorEstado(EstadoSolicitud.APROBADA);
        long rechazadas = solicitudService.contarPorEstado(EstadoSolicitud.RECHAZADA);

        model.addAttribute("total", total);
        model.addAttribute("pendientes", pendientes);
        model.addAttribute("aprobadas", aprobadas);
        model.addAttribute("rechazadas", rechazadas);
        model.addAttribute("solicitudes", solicitudService.obtenerTodas());

        return "admin/solicitudes-panel";
    }
}
