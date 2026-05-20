package com.cronos.gestiontributaria.notification.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.util.List;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.cronos.gestiontributaria.auth.model.User;
import com.cronos.gestiontributaria.clientes.model.TaxPayer;
import com.cronos.gestiontributaria.obligaciones.dto.TaxObligationResponseDTO;

/**
 * Servicio de envío de correos electrónicos con plantilla HTML profesional
 * alineada a la identidad visual de Cronos.
 */
@Service
public class NotificationMailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String mailUsername;
    private final String fromAddress;
    private final String appBaseUrl;

    public NotificationMailService(@org.springframework.beans.factory.annotation.Autowired(required = false) JavaMailSender mailSender,
                                   TemplateEngine templateEngine,
                                   @Value("${spring.mail.username:}") String mailUsername,
                                   @Value("${app.mail.from:}") String fromAddress,
                                   @Value("${app.base-url:http://localhost:8080}") String appBaseUrl) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.mailUsername = mailUsername;
        this.fromAddress = fromAddress;
        this.appBaseUrl = appBaseUrl;
    }

    /**
     * Envía un correo HTML con la plantilla Cronos.
     */
    public void sendEmail(String recipientEmail, String subject, String message) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new IllegalArgumentException("El destinatario del correo es obligatorio");
        }

        String resolvedSubject = subject != null && !subject.isBlank() ? subject.trim() : "Notificación Cronos";
        
        Context context = new Context();
        context.setVariable("title", resolvedSubject);
        context.setVariable("headline", resolvedSubject);
        context.setVariable("intro", message != null && !message.isBlank() ? message : "Sin contenido.");
        context.setVariable("details", List.of());
        context.setVariable("actionLabel", null);
        context.setVariable("actionPath", null);
        context.setVariable("footerNote", "Mensaje enviado automáticamente por Cronos.");

        String htmlBody = templateEngine.process("email/structured-notification", context);
        sendHtml(recipientEmail.trim(), resolvedSubject, htmlBody);
    }

    public void sendStructuredEmail(String recipientEmail,
                                    String subject,
                                    String headline,
                                    String intro,
                                    List<NotificationEmailDetail> details,
                                    String actionLabel,
                                    String actionPath) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new IllegalArgumentException("El destinatario del correo es obligatorio");
        }

        String resolvedSubject = subject != null && !subject.isBlank() ? subject.trim() : "Notificación Cronos";
        
        Context context = new Context();
        context.setVariable("title", resolvedSubject);
        context.setVariable("headline", headline != null && !headline.isBlank() ? headline : resolvedSubject);
        context.setVariable("intro", intro);
        context.setVariable("details", details != null ? details : List.of());
        context.setVariable("actionLabel", actionLabel);
        context.setVariable("actionPath", buildAbsoluteUrl(actionPath));
        context.setVariable("footerNote", "Mensaje enviado automáticamente por Cronos.");

        String htmlBody = templateEngine.process("email/structured-notification", context);
        sendHtml(recipientEmail.trim(), resolvedSubject, htmlBody);
    }

    public void sendObligationCreated(TaxPayer taxpayer, TaxObligationResponseDTO obligation) {
        if (taxpayer == null || taxpayer.getEmail() == null || taxpayer.getEmail().isBlank()) {
            return;
        }

        String subject = "Nueva obligación registrada — Cronos";
        sendStructuredEmail(
                taxpayer.getEmail(),
                subject,
                "Nueva obligación registrada",
                "Se ha registrado una nueva obligación tributaria en tu cuenta de Cronos.",
                List.of(
                    new NotificationEmailDetail("Cliente", taxpayer.getBusinessName()),
                    new NotificationEmailDetail("Identificación", taxpayer.getIdentificacion()),
                    new NotificationEmailDetail("Tipo", obligation != null && obligation.type() != null && obligation.type().getDescription() != null
                                ? obligation.type().getDescription() : (obligation != null && obligation.type() != null ? obligation.type().name() : "—")),
                    new NotificationEmailDetail("Periodo fiscal", obligation != null && obligation.fiscalPeriod() != null ? obligation.fiscalPeriod() : "—"),
                    new NotificationEmailDetail("Año gravable", obligation != null ? String.valueOf(obligation.taxYear()) : "—"),
                    new NotificationEmailDetail("Vencimiento", obligation != null && obligation.dueDate() != null ? obligation.dueDate().toString() : "—")
                ),
                "Ver obligación",
                obligation != null && obligation.id() != null ? "/portal/obligaciones/" + obligation.id() : "/portal/obligaciones");
    }

    public void sendObligationStatusChanged(TaxPayer taxpayer, TaxObligationResponseDTO obligation) {
        if (taxpayer == null || taxpayer.getEmail() == null || taxpayer.getEmail().isBlank()) {
            return;
        }

        String statusLabel = switch (obligation.status()) {
            case PENDING -> "Pendiente";
            case IN_PROGRESS -> "En progreso";
            case COMPLETED -> "Completado";
            case OVERDUE -> "Vencido";
            case CANCELLED -> "Cancelado";
        };

        String accentColor = switch (obligation.status()) {
            case COMPLETED -> "#22c55e";
            case OVERDUE -> "#ef4444";
            case IN_PROGRESS -> "#4f8cff";
            default -> "#f59e0b";
        };

        String subject = "Actualización de obligación — Cronos";
        sendStructuredEmail(
            taxpayer.getEmail(),
            subject,
            "Estado actualizado: " + statusLabel,
            "El estado de una obligación tributaria ha sido actualizado en Cronos.",
            List.of(
                new NotificationEmailDetail("Cliente", taxpayer.getBusinessName()),
                new NotificationEmailDetail("Identificación", taxpayer.getIdentificacion()),
                new NotificationEmailDetail("Tipo", obligation != null && obligation.type() != null && obligation.type().getDescription() != null
                    ? obligation.type().getDescription() : (obligation != null && obligation.type() != null ? obligation.type().name() : "—")),
                new NotificationEmailDetail("Periodo fiscal", obligation != null && obligation.fiscalPeriod() != null ? obligation.fiscalPeriod() : "—"),
                new NotificationEmailDetail("Año gravable", obligation != null ? String.valueOf(obligation.taxYear()) : "—"),
                new NotificationEmailDetail("Vencimiento", obligation != null && obligation.dueDate() != null ? obligation.dueDate().toString() : "—"),
                new NotificationEmailDetail("Estado", statusLabel)
            ),
            "Ver obligación",
            obligation != null && obligation.id() != null ? "/portal/obligaciones/" + obligation.id() : "/portal/obligaciones");
    }

    public void sendReminderEmail(User user, String recipientEmail, String subject, String message) {
        String resolvedSubject = subject != null && !subject.isBlank() ? subject : "Recordatorio — Cronos";
        String resolvedMessage = message != null && !message.isBlank()
                ? message
                : (user != null ? "Recordatorio enviado por " + user.getName() : "Recordatorio Cronos");
        String senderName = user != null && user.getName() != null ? user.getName() : "Equipo Cronos";

        sendStructuredEmail(
            recipientEmail,
            resolvedSubject,
            "Recordatorio Cronos",
            resolvedMessage,
                List.of(new NotificationEmailDetail("Enviado por", senderName)),
            "Abrir sistema",
            "/notificaciones");
    }

    // ─── Private helpers ──────────────────────────────────────────

    private void sendHtml(String to, String subject, String htmlContent) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(to);
            if (mailUsername != null && !mailUsername.isBlank()) {
                helper.setFrom(mailUsername.trim());
            }
            if (fromAddress != null && !fromAddress.isBlank() && !fromAddress.trim().equalsIgnoreCase(mailUsername)) {
                helper.setReplyTo(fromAddress.trim());
            }
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar correo HTML: " + e.getMessage(), e);
        }
    }

    // ─── HTML builders ────────────────────────────────────────────

    private String buildAbsoluteUrl(String path) {
        if (path == null || path.isBlank()) {
            return appBaseUrl;
        }
        String normalizedBase = appBaseUrl != null ? appBaseUrl.trim() : "http://localhost:8080";
        if (normalizedBase.endsWith("/")) {
            normalizedBase = normalizedBase.substring(0, normalizedBase.length() - 1);
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        return normalizedBase + (path.startsWith("/") ? path : "/" + path);
    }
}