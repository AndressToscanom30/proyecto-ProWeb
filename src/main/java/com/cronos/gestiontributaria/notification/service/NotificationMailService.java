package com.cronos.gestiontributaria.notification.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import com.cronos.gestiontributaria.auth.model.User;
import com.cronos.gestiontributaria.clientes.model.TaxPayer;
import com.cronos.gestiontributaria.obligaciones.dto.TaxObligationResponseDTO;

/**

 * Documentación de la entidad NotificationMailService.

 */

@Service
public class NotificationMailService {

    private final JavaMailSender mailSender;
    private final String mailUsername;
    private final String fromAddress;

    public NotificationMailService(JavaMailSender mailSender,
                                   @Value("${spring.mail.username:}") String mailUsername,
                                   @Value("${app.mail.from:}") String fromAddress) {
        this.mailSender = mailSender;
        this.mailUsername = mailUsername;
        this.fromAddress = fromAddress;
    }

    public void sendEmail(String recipientEmail, String subject, String message) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new IllegalArgumentException("El destinatario del correo es obligatorio");
        }

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(recipientEmail.trim());
        if (mailUsername != null && !mailUsername.isBlank()) {
            mailMessage.setFrom(mailUsername.trim());
        }
        if (fromAddress != null && !fromAddress.isBlank() && !fromAddress.trim().equalsIgnoreCase(mailUsername)) {
            mailMessage.setReplyTo(fromAddress.trim());
        }
        mailMessage.setSubject(subject != null && !subject.isBlank() ? subject.trim() : "Notificación TaxControl");
        mailMessage.setText(message != null && !message.isBlank() ? message : "Sin contenido.");
        mailSender.send(mailMessage);
    }

    public void sendObligationCreated(TaxPayer taxpayer, TaxObligationResponseDTO obligation) {
        if (taxpayer == null || taxpayer.getEmail() == null || taxpayer.getEmail().isBlank()) {
            return;
        }

        String subject = "Nueva obligación registrada en TaxControl";
        String message = buildObligationMessage(taxpayer, obligation, "Se registró una nueva obligación");
        sendEmail(taxpayer.getEmail(), subject, message);
    }

    public void sendObligationStatusChanged(TaxPayer taxpayer, TaxObligationResponseDTO obligation) {
        if (taxpayer == null || taxpayer.getEmail() == null || taxpayer.getEmail().isBlank()) {
            return;
        }

        String subject = "Actualización de obligación en TaxControl";
        String message = buildObligationMessage(taxpayer, obligation,
                "El estado de una obligación cambió a " + obligation.status());
        sendEmail(taxpayer.getEmail(), subject, message);
    }

    public void sendReminderEmail(User user, String recipientEmail, String subject, String message) {
        String resolvedSubject = subject != null && !subject.isBlank() ? subject : "Recordatorio TaxControl";
        String resolvedMessage = message != null && !message.isBlank()
                ? message
                : (user != null ? "Recordatorio enviado por " + user.getName() : "Recordatorio TaxControl");
        sendEmail(recipientEmail, resolvedSubject, resolvedMessage);
    }

    private String buildObligationMessage(TaxPayer taxpayer, TaxObligationResponseDTO obligation, String headline) {
        StringBuilder builder = new StringBuilder();
        builder.append(headline).append("\n\n");
        builder.append("Cliente: ").append(taxpayer.getBusinessName()).append("\n");
        builder.append("Identificación: ").append(taxpayer.getIdentificacion()).append("\n");
        builder.append("Tipo: ").append(obligation.type()).append("\n");
        builder.append("Periodo: ").append(obligation.fiscalPeriod()).append("\n");
        builder.append("Año: ").append(obligation.taxYear()).append("\n");
        builder.append("Vencimiento: ").append(obligation.dueDate()).append("\n");
        builder.append("Estado: ").append(obligation.status()).append("\n\n");
        builder.append("Revisa TaxControl para gestionar el seguimiento.");
        return builder.toString();
    }
}