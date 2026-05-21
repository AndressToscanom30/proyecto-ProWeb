package com.cronos.gestiontributaria.config;

import java.time.LocalDate;
import java.util.ArrayList;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.cronos.gestiontributaria.auth.model.Role;
import com.cronos.gestiontributaria.auth.model.User;
import com.cronos.gestiontributaria.auth.repository.UserRepository;
import com.cronos.gestiontributaria.empleados.model.Employee;

/**

 * Documentación de la entidad DataInitializer.

 */

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        java.util.Optional<User> optGerente = userRepository.findByEmail("gerente@cronos.com");
        if (optGerente.isEmpty()) {
            User gerente = new User();
            gerente.setName("Gerente General");
            gerente.setEmail("gerente@cronos.com");
            gerente.setPasswordHash(passwordEncoder.encode("admin123"));
            gerente.setActive(true);
            gerente.setRole(new Role("ROLE_GERENTE", "Gerente", new ArrayList<>()));
            gerente.setNotifications(new ArrayList<>());
            userRepository.save(gerente);
            System.out.println(">>> GERENTE por defecto creado: gerente@cronos.com / admin123");
        } else {
            User gerente = optGerente.get();
            if (gerente.getRole() == null || !"ROLE_GERENTE".equals(gerente.getRole().getName())) {
                gerente.setRole(new Role("ROLE_GERENTE", "Gerente", new ArrayList<>()));
                userRepository.save(gerente);
                System.out.println(">>> Rol de GERENTE restaurado para gerente@cronos.com");
            }
        }

        // Usuario de prueba para el portal del contribuyente.
        // taxPayerId queda como placeholder hasta que se cree un TaxPayer real
        // (el vínculo automático se implementa en B-3).
        boolean hasContribuyente = userRepository.findByEmail("contribuyente@cronos.com").isPresent();
        if (!hasContribuyente) {
            User contribuyente = new User();
            contribuyente.setName("Contribuyente Demo");
            contribuyente.setEmail("contribuyente@cronos.com");
            contribuyente.setPasswordHash(passwordEncoder.encode("test1234"));
            contribuyente.setActive(true);
            contribuyente.setRole(Role.contribuyente());
            contribuyente.setTaxPayerId("test-taxpayer-id");
            contribuyente.setNotifications(new ArrayList<>());
            userRepository.save(contribuyente);
            System.out.println(">>> CONTRIBUYENTE por defecto creado: contribuyente@cronos.com / test1234");
        }

        // Empleado de prueba con rol AUXILIAR_CONTADOR.
        // Se persiste como Employee (no como User plano) para que el módulo
        // de empleados lo encuentre (EmployeeService.findAll() filtra por
        // instanceof Employee). Si ya existía como User plano por una
        // ejecución previa, se elimina y se vuelve a crear como Employee.
        java.util.Optional<User> optAuxiliar = userRepository.findByEmail("auxiliar@cronos.com");
        if (optAuxiliar.isEmpty()) {
            crearEmpleadoAuxiliar();
        } else if (!(optAuxiliar.get() instanceof Employee)) {
            // Migración: el usuario existe pero como User plano → reemplazar
            // por Employee respetando el id existente.
            User existing = optAuxiliar.get();
            userRepository.deleteById(existing.getId());
            Employee auxiliar = nuevoEmpleadoAuxiliar();
            auxiliar.setId(existing.getId());
            userRepository.save(auxiliar);
            System.out.println(">>> AUXILIAR_CONTADOR migrado a Employee: auxiliar@cronos.com");
        }
    }

    private void crearEmpleadoAuxiliar() {
        Employee auxiliar = nuevoEmpleadoAuxiliar();
        userRepository.save(auxiliar);
        System.out.println(">>> AUXILIAR_CONTADOR (Employee) creado: auxiliar@cronos.com / admin123");
    }

    private Employee nuevoEmpleadoAuxiliar() {
        Role role = new Role("ROLE_AUXILIAR_CONTADOR",
                "Auxiliar de contador", new ArrayList<>());
        Employee auxiliar = new Employee(
                "Auxiliar Contable",
                "auxiliar@cronos.com",
                passwordEncoder.encode("admin123"),
                true,
                role,
                new ArrayList<>(),
                "Auxiliar contable",      // position
                "3000000000",             // phone (placeholder)
                LocalDate.now(),          // hireDate
                new ArrayList<>(),        // tasks
                new ArrayList<>());       // obligations
        return auxiliar;
    }
}
