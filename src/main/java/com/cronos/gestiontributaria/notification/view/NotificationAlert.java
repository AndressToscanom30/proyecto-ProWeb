package com.cronos.gestiontributaria.notification.view;

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