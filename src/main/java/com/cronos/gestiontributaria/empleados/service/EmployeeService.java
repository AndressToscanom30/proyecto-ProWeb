package com.cronos.gestiontributaria.empleados.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.cronos.gestiontributaria.auth.model.Role;
import com.cronos.gestiontributaria.auth.model.User;
import com.cronos.gestiontributaria.auth.repository.UserRepository;
import com.cronos.gestiontributaria.common.EmployeeRole;
import com.cronos.gestiontributaria.empleados.model.Employee;

@Service
public class EmployeeService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Employee> findAll() {
        return userRepository.findAll().stream()
                .filter(u -> u instanceof Employee)
                .map(u -> (Employee) u)
                .toList();
    }

    public Employee findById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Empleado no encontrado con ID: " + id));
        if (!(user instanceof Employee)) {
            throw new NoSuchElementException("El usuario con ID " + id + " no es un empleado");
        }
        return (Employee) user;
    }

    public Employee create(String name, String email, String password, EmployeeRole role,
                            String phone, String position) {
        if (userRepository.existsByEmail(email.trim().toLowerCase())) {
            throw new IllegalArgumentException("El correo ya está registrado");
        }

        Employee employee = new Employee(name, email.trim().toLowerCase(),
                passwordEncoder.encode(password), true,
                buildRole(role), new ArrayList<>(),
                position != null ? position : role.name(),
                phone, LocalDate.now(),
                new ArrayList<>(), new ArrayList<>());

        return (Employee) userRepository.save(employee);
    }

    public void delete(String id) {
        if (!userRepository.existsById(id)) {
            throw new NoSuchElementException("Empleado no encontrado con ID: " + id);
        }
        userRepository.deleteById(id);
    }

    public long count() {
        return userRepository.findAll().stream()
                .filter(u -> u instanceof Employee)
                .count();
    }

    private Role buildRole(EmployeeRole employeeRole) {
        String roleName = "ROLE_" + employeeRole.name();
        String description = switch (employeeRole) {
            case GERENTE -> "Gerente";
            case CONTADOR -> "Contador";
            case AUXILIAR_CONTADOR -> "Auxiliar de contador";
            case CONTRIBUYENTE -> "Contribuyente";
        };
        return new Role(roleName, description, new ArrayList<>());
    }
}
