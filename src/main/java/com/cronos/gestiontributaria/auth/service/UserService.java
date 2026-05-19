package com.cronos.gestiontributaria.auth.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.cronos.gestiontributaria.auth.model.Role;
import com.cronos.gestiontributaria.auth.model.User;
import com.cronos.gestiontributaria.auth.repository.UserRepository;

/**
 * Servicio de negocio para el registro y la consulta de usuarios autenticables.
 *
 * <p>Se encarga de validar duplicados, cifrar la contraseña con BCrypt y asignar
 * el rol por defecto al registrar nuevos usuarios.</p>
 */
@Service
public class UserService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Construye el servicio con el repositorio Mongo y el codificador de contraseñas.
     *
     * @param repository repositorio de usuarios
     * @param passwordEncoder encoder BCrypt usado para registrar contraseñas seguras
     */
    public UserService(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registra un usuario nuevo en la base de datos.
     *
     * <p>Normaliza el correo, verifica duplicados, cifra la contraseña y
     * asigna el rol {@code ROLE_USER} por defecto.</p>
     *
     * @param user usuario a registrar
     * @return usuario persistido
     */
    public User register(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException("El correo es obligatorio");
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }

        String normalizedEmail = normalizeEmail(user.getEmail());
        if (repository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("El correo ya está registrado");
        }

        if (user.getName() != null) {
            user.setName(user.getName().trim());
        }
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(user.getPassword()));
        user.setPassword(null);
        user.setActive(true);
        user.setRole(defaultUserRole());

        if (user.getNotifications() == null) {
            user.setNotifications(new ArrayList<>());
        }

        return repository.save(user);
    }

    /**
     * Lista todos los usuarios ordenados por nombre.
     *
     * @return lista de usuarios
     */
    public List<User> findAll() {
        return repository.findAllByOrderByNameAsc();
    }

    /**
     * Busca un usuario por correo.
     *
     * @param email correo a consultar
     * @return usuario encontrado, si existe
     */
    public Optional<User> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        return repository.findByEmail(normalizeEmail(email));
    }

    /**
     * Normaliza un correo a minúsculas y sin espacios extra.
     *
     * @param email correo original
     * @return correo normalizado
     */
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Construye el rol por defecto para los usuarios registrados.
     *
     * @return rol estándar de usuario
     */
    private Role defaultUserRole() {
        return new Role("ROLE_USER", "Usuario estándar", new ArrayList<>());
    }

    /**
     * Asigna rol CONTRIBUYENTE y vincula el User al TaxPayer indicado.
     *
     * <p>Si no existe un User con ese email, no hace nada (el vínculo
     * se puede crear después cuando el contribuyente se registre).</p>
     *
     * @param email correo del usuario a vincular
     * @param taxPayerId ID del {@code TaxPayer} a asociar
     */
    public void vincularContribuyente(String email, String taxPayerId) {
        if (email == null || email.isBlank()) {
            return;
        }
        repository.findByEmail(normalizeEmail(email)).ifPresent(user -> {
            if (user.getRole() == null || "ROLE_USER".equals(user.getRole().getName())) {
                user.setRole(Role.contribuyente());
            }
            user.setTaxPayerId(taxPayerId);
            repository.save(user);
        });
    }
}