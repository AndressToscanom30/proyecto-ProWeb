package com.cronos.gestiontributaria.mensajes.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.cronos.gestiontributaria.auth.model.User;
import com.cronos.gestiontributaria.auth.service.UserService;
import com.cronos.gestiontributaria.mensajes.service.MensajeService;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/ui/mensajes")
public class MensajeUIController {

    private final MensajeService mensajeService;
    private final UserService userService;

    public MensajeUIController(MensajeService mensajeService, UserService userService) {
        this.mensajeService = mensajeService;
        this.userService = userService;
    }

    @GetMapping
    public String bandejaEntrada(Model model, Authentication authentication) {
        if (authentication != null && authentication.getName() != null) {
            userService.findByEmail(authentication.getName())
                    .ifPresent(user -> model.addAttribute("usuario", user));
        } else {
            model.addAttribute("usuario", new User());
        }

        model.addAttribute("recibidos", mensajeService.obtenerBandejaEntrada(authentication.getName()));
        model.addAttribute("enviados", mensajeService.obtenerEnviados(authentication.getName()));
        model.addAttribute("noLeidos", mensajeService.contarNoLeidos(authentication.getName()));
        
        // Lista de usuarios para el select del destinatario (excluyendo al usuario actual)
        List<User> destinatarios = userService.findAll().stream()
                .filter(u -> !u.getEmail().equalsIgnoreCase(authentication.getName()))
                .collect(Collectors.toList());
        model.addAttribute("usuarios", destinatarios);
        
        return "admin/mensajes/panel"; // We'll create this template
    }
}
