package com.cronos.gestiontributaria.obligaciones.model;

import java.time.LocalDateTime;

/**
 * Información de una asignación de responsable sobre una obligación.
 */
public class ObligationAssignment {

    private String userId;
    private String userName;
    private String userEmail;
    private String assignedById;
    private String assignedByName;
    private String assignedByEmail;
    private LocalDateTime assignedAt;

    public ObligationAssignment() {
    }

    public ObligationAssignment(String userId, String userName, String userEmail,
            String assignedById, String assignedByName, String assignedByEmail,
            LocalDateTime assignedAt) {
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.assignedById = assignedById;
        this.assignedByName = assignedByName;
        this.assignedByEmail = assignedByEmail;
        this.assignedAt = assignedAt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getAssignedById() {
        return assignedById;
    }

    public void setAssignedById(String assignedById) {
        this.assignedById = assignedById;
    }

    public String getAssignedByName() {
        return assignedByName;
    }

    public void setAssignedByName(String assignedByName) {
        this.assignedByName = assignedByName;
    }

    public String getAssignedByEmail() {
        return assignedByEmail;
    }

    public void setAssignedByEmail(String assignedByEmail) {
        this.assignedByEmail = assignedByEmail;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }
}