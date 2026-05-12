package com.cronos.gestiontributaria.notification.view;

/**

 * Documentación de la entidad NotificationAlert.

 */

public record NotificationAlert(
        String clientName,
        String clientIdentification,
        String clientEmail,
        String obligationLabel,
        String dueDateLabel,
        String statusLabel,
        String tone,
        String subject,
        String body) {
}