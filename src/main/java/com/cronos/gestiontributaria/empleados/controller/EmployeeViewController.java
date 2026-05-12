package com.cronos.gestiontributaria.empleados.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.cronos.gestiontributaria.auth.model.User;
import com.cronos.gestiontributaria.auth.service.UserService;
import com.cronos.gestiontributaria.common.EmployeeRole;
import com.cronos.gestiontributaria.empleados.service.EmployeeService;

@Controller
@RequestMapping("/empleados")
public class EmployeeViewController {

    private final EmployeeService employeeService;
    private final UserService userService;

    public EmployeeViewController(EmployeeService employeeService, UserService userService) {
        this.employeeService = employeeService;
        this.userService = userService;
    }

    @GetMapping
    public String list(Model model, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        model.addAttribute("usuario", user);
        model.addAttribute("empleados", employeeService.findAll());
        return "empleados/list";
    }

    @GetMapping("/nuevo")
    public String createForm(Model model, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        model.addAttribute("usuario", user);
        model.addAttribute("roles", EmployeeRole.values());
        return "empleados/form";
    }

    @PostMapping("/guardar")
    public String save(@RequestParam String name,
                        @RequestParam String email,
                        @RequestParam String password,
                        @RequestParam EmployeeRole role,
                        @RequestParam(required = false) String phone,
                        @RequestParam(required = false) String position,
                        Model model, Authentication authentication) {
        try {
            employeeService.create(name, email, password, role, phone, position);
            return "redirect:/empleados";
        } catch (IllegalArgumentException e) {
            User user = userService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
            model.addAttribute("usuario", user);
            model.addAttribute("roles", EmployeeRole.values());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("name", name);
            model.addAttribute("email", email);
            model.addAttribute("role", role);
            model.addAttribute("phone", phone);
            model.addAttribute("position", position);
            return "empleados/form";
        }
    }

    @GetMapping("/eliminar/{id}")
    public String delete(@PathVariable String id) {
        employeeService.delete(id);
        return "redirect:/empleados";
    }
}
