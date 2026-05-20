package com.cronos.gestiontributaria.obligaciones.dto;

import java.time.LocalDateTime;

/**
 * DTO con el detalle de una asignación de responsable.
 */
public record ObligationAssignmentDTO(
        String userId,
        String userName,
        String userEmail,
        String assignedById,
        String assignedByName,
        String assignedByEmail,
        LocalDateTime assignedAt) {
}