package com.cronos.gestiontributaria.auth.model;

import java.util.ArrayList;
import java.util.List;

import com.cronos.gestiontributaria.notification.model.Notification;

public class User {
    private String name;
    private String email;
    private String passwordHash;
    private boolean active;
    private Role role;
    private List<Notification> notifications;

    public User(){

    }

    public User(String name, String email, String passwordHash, boolean active, Role role,
                List<Notification> notifications) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.active = active;
        this.role = role;
        this.notifications = notifications != null ? notifications : new ArrayList<>();
    }

    public void changePassword(String newPassword) {
        if (newPassword != null && !newPassword.isEmpty()) {
            this.passwordHash = newPassword;
        }
    }

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

    public List<Notification> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<Notification> notifications) {
        this.notifications = notifications != null ? notifications : new ArrayList<>();
    }

}
