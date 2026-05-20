package com.cronos.gestiontributaria.auth.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.cronos.gestiontributaria.notification.model.Notification;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Representa un usuario autenticable persistido en MongoDB.
 *
 * <p>La contraseña real se guarda en {@code passwordHash}; el campo
 * {@code password} se usa únicamente como dato temporal para el registro.</p>
 */
@Document(collection = "usuarios")
public class User {
    @Id
    private String id;

    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    @Email(message = "Debe ser un correo válido")
    @NotBlank(message = "El correo es obligatorio")
    private String email;

    private String passwordHash;

    @Transient
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    private boolean active = true;
    private Role role;

    /**
     * ID del {@code TaxPayer} asociado. Solo aplica cuando el rol del usuario
     * es CONTRIBUYENTE. Es {@code null} para usuarios GERENTE y ASESOR.
     */
    private String taxPayerId;

    private String resetToken;
    private LocalDateTime resetTokenExpiry;

    private List<Notification> notifications = new ArrayList<>();

    /**
     * Crea un usuario vacío con estado activo y lista de notificaciones inicializada.
     */
    public User(){
        this.notifications = new ArrayList<>();
        this.active = true;
    }

    /**
     * Crea un usuario con sus datos base y colecciones relacionadas.
     *
     * @param name nombre visible del usuario
     * @param email correo del usuario
     * @param passwordHash contraseña cifrada
     * @param active indica si la cuenta está activa
     * @param role rol asignado
     * @param notifications notificaciones asociadas
     */
    public User(String name, String email, String passwordHash, boolean active, Role role,
                List<Notification> notifications) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.active = active;
        this.role = role;
        this.notifications = notifications != null ? notifications : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Reemplaza la contraseña cifrada con un nuevo valor.
     *
     * @param newPassword nuevo hash o valor de contraseña
     */
    public void changePassword(String newPassword) {
        if (newPassword != null && !newPassword.isEmpty()) {
            this.passwordHash = newPassword;
        }
    }

    /**
     * Reactiva la cuenta si el correo recibido coincide con el correo del usuario.
     *
     * @param email correo para validar la recuperación
     */
    public void recoverPassword(String email) {
        if (email != null && this.email != null && this.email.equalsIgnoreCase(email)) {
            this.active = true;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getTaxPayerId() {
        return taxPayerId;
    }

    public void setTaxPayerId(String taxPayerId) {
        this.taxPayerId = taxPayerId;
    }

    public List<Notification> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<Notification> notifications) {
        this.notifications = notifications != null ? notifications : new ArrayList<>();
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public LocalDateTime getResetTokenExpiry() {
        return resetTokenExpiry;
    }

    public void setResetTokenExpiry(LocalDateTime resetTokenExpiry) {
        this.resetTokenExpiry = resetTokenExpiry;
    }

}
