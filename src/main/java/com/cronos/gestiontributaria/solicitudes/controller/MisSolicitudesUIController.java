package com.cronos.gestiontributaria.solicitudes.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.cronos.gestiontributaria.auth.model.User;
import com.cronos.gestiontributaria.auth.service.UserService;
import com.cronos.gestiontributaria.solicitudes.model.TipoSolicitud;
import com.cronos.gestiontributaria.solicitudes.service.SolicitudService;

@Controller
@RequestMapping("/ui/solicitudes")
public class MisSolicitudesUIController {

    private final SolicitudService solicitudService;
    private final UserService userService;

    public MisSolicitudesUIController(SolicitudService solicitudService, UserService userService) {
        this.solicitudService = solicitudService;
        this.userService = userService;
    }

    @GetMapping("/mis-solicitudes")
    public String misSolicitudes(Model model, Authentication authentication) {
        if (authentication != null && authentication.getName() != null) {
            userService.findByEmail(authentication.getName())
                    .ifPresent(user -> model.addAttribute("usuario", user));
        } else {
            model.addAttribute("usuario", new User());
        }

        model.addAttribute("solicitudes", solicitudService.listarPorUsuario(authentication.getName()));
        model.addAttribute("tipos", TipoSolicitud.values());
        
        return "admin/solicitudes/mis_solicitudes"; // Template to create
    }
}
