package com.cronos.gestiontributaria.config;

import java.util.ArrayList;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.cronos.gestiontributaria.auth.model.Role;
import com.cronos.gestiontributaria.auth.model.User;
import com.cronos.gestiontributaria.auth.repository.UserRepository;

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
    }
}
