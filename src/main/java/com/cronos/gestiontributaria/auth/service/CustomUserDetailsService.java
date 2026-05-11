package com.cronos.gestiontributaria.auth.service;

import java.util.Locale;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.cronos.gestiontributaria.auth.model.User;
import com.cronos.gestiontributaria.auth.repository.UserRepository;

/**
 * Implementación de {@link UserDetailsService} que carga usuarios desde MongoDB.
 *
 * <p>Spring Security usa este servicio durante el login por formulario para
 * obtener contraseña, rol y estado de la cuenta.</p>
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository repository;

    /**
     * Crea el servicio con el repositorio de usuarios.
     *
     * @param repository repositorio MongoDB
     */
    public CustomUserDetailsService(UserRepository repository) {
        this.repository = repository;
    }

    /**
     * Carga el usuario autenticable a partir del correo.
     *
     * @param email correo usado como nombre de usuario
     * @return detalles del usuario para Spring Security
     * @throws UsernameNotFoundException si no se encuentra un usuario con ese correo
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = repository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));

        String roleName = "ROLE_USER";
        if (user.getRole() != null && user.getRole().getName() != null && !user.getRole().getName().isBlank()) {
            roleName = user.getRole().getName().trim().toUpperCase(Locale.ROOT);
        }
        if (!roleName.startsWith("ROLE_")) {
            roleName = "ROLE_" + roleName;
        }

        String authority = roleName.substring("ROLE_".length());

        return org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .roles(authority)
                .disabled(!user.isActive())
                .build();
    }

    /**
     * Normaliza el correo recibido antes de consultar MongoDB.
     *
     * @param email correo original
     * @return correo normalizado
     */
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}