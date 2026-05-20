package com.cronos.gestiontributaria.auth.controller;

import java.util.Optional;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.cronos.gestiontributaria.auth.model.User;
import com.cronos.gestiontributaria.auth.service.UserService;
import com.cronos.gestiontributaria.common.service.EmailService;

@Controller
public class PasswordController {

    private final UserService userService;
    private final EmailService emailService;

    public PasswordController(UserService userService, EmailService emailService) {
        this.userService = userService;
        this.emailService = emailService;
    }

    // --- FORGOT PASSWORD (PUBLIC) ---

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("email") String email, Model model) {
        Optional<User> optionalUser = userService.findByEmail(email);
        
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            String token = userService.createPasswordResetTokenForUser(user);
            emailService.sendPasswordResetMail(user.getEmail(), token);
        }

        // Siempre mostramos el mismo mensaje por seguridad (evitar enumeración de usuarios)
        model.addAttribute("success", "Si el correo está registrado, te hemos enviado un enlace para recuperar tu contraseña.");
        return "auth/forgot-password";
    }

    // --- RESET PASSWORD (PUBLIC - FROM EMAIL) ---

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam("token") String token, Model model) {
        Optional<User> user = userService.validatePasswordResetToken(token);
        if (user.isEmpty()) {
            model.addAttribute("error", "El enlace de recuperación es inválido o ha expirado.");
            return "auth/reset-password";
        }
        
        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("token") String token, 
                                     @RequestParam("password") String newPassword, 
                                     @RequestParam("confirmPassword") String confirmPassword,
                                     Model model) {
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Las contraseñas no coinciden.");
            model.addAttribute("token", token);
            return "auth/reset-password";
        }

        Optional<User> optionalUser = userService.validatePasswordResetToken(token);
        if (optionalUser.isEmpty()) {
            model.addAttribute("error", "El enlace de recuperación es inválido o ha expirado.");
            return "auth/reset-password";
        }

        User user = optionalUser.get();
        userService.changeUserPassword(user, newPassword);

        model.addAttribute("success", "Tu contraseña ha sido actualizada con éxito. Ya puedes iniciar sesión.");
        return "auth/login"; // Redirigimos al login con el mensaje de éxito
    }

    // --- CHANGE PASSWORD (PRIVATE - LOGGED IN) ---

    @GetMapping("/profile/change-password")
    @PreAuthorize("isAuthenticated()")
    public String showChangePasswordForm() {
        return "auth/change-password";
    }

    @PostMapping("/profile/change-password")
    @PreAuthorize("isAuthenticated()")
    public String processChangePassword(Authentication authentication,
                                        @RequestParam("oldPassword") String oldPassword,
                                        @RequestParam("newPassword") String newPassword,
                                        @RequestParam("confirmPassword") String confirmPassword,
                                        Model model) {
                                            
        String email = authentication.getName();
        Optional<User> optionalUser = userService.findByEmail(email);
        
        if (optionalUser.isEmpty()) {
            return "redirect:/login";
        }

        User user = optionalUser.get();

        if (!userService.checkIfValidOldPassword(user, oldPassword)) {
            model.addAttribute("error", "La contraseña actual es incorrecta.");
            return "auth/change-password";
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Las nuevas contraseñas no coinciden.");
            return "auth/change-password";
        }

        userService.changeUserPassword(user, newPassword);
        model.addAttribute("success", "Tu contraseña ha sido actualizada correctamente.");
        return "auth/change-password";
    }
}
