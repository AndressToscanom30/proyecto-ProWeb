package com.cronos.gestiontributaria.auth.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

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
     * Lista usuarios por rol persistido exacto.
     *
     * @param roleName nombre persistido del rol
     * @return usuarios con el rol solicitado
     */
    public List<User> findByRoleName(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return List.of();
        }
        return repository.findByRole_Name(roleName.trim().toUpperCase(Locale.ROOT));
    }

    /**
     * Lista usuarios por conjunto de roles persistidos.
     *
     * @param roleNames nombres persistidos de los roles
     * @return usuarios con cualquiera de los roles solicitados
     */
    public List<User> findByRoleNames(Collection<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return List.of();
        }
        List<String> normalized = roleNames.stream()
                .filter(roleName -> roleName != null && !roleName.isBlank())
                .map(roleName -> roleName.trim().toUpperCase(Locale.ROOT))
                .toList();
        if (normalized.isEmpty()) {
            return List.of();
        }
        return repository.findByRole_NameIn(normalized);
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
            user.setRole(Role.contribuyente());
            user.setTaxPayerId(taxPayerId);
            repository.save(user);
        });
    }

    /**
     * Genera un token de recuperación para un usuario y establece su expiración a 24 horas.
     * @param user usuario al que se le genera el token
     * @return el token generado
     */
    public String createPasswordResetTokenForUser(User user) {
        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(24));
        repository.save(user);
        return token;
    }

    /**
     * Valida un token de recuperación.
     * @param token el token a validar
     * @return el usuario si el token es válido y no ha expirado, Optional.empty() en caso contrario.
     */
    public Optional<User> validatePasswordResetToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        Optional<User> optionalUser = repository.findByResetToken(token);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.getResetTokenExpiry() != null && user.getResetTokenExpiry().isAfter(LocalDateTime.now())) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    /**
     * Cambia la contraseña de un usuario, la cifra y elimina el token de recuperación si existe.
     * @param user usuario al que se le cambiará la clave
     * @param newPassword la nueva contraseña en texto plano
     */
    public void changeUserPassword(User user, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("La nueva contraseña no puede estar vacía");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        repository.save(user);
    }
    
    /**
     * Verifica si una contraseña coincide con el hash del usuario.
     */
    public boolean checkIfValidOldPassword(User user, String oldPassword) {
        return passwordEncoder.matches(oldPassword, user.getPasswordHash());
    }
}