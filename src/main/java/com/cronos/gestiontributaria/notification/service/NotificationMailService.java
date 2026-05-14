package com.cronos.gestiontributaria.notification.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

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
    private final String mailUsername;
    private final String fromAddress;

    public NotificationMailService(JavaMailSender mailSender,
                                   @Value("${spring.mail.username:}") String mailUsername,
                                   @Value("${app.mail.from:}") String fromAddress) {
        this.mailSender = mailSender;
        this.mailUsername = mailUsername;
        this.fromAddress = fromAddress;
    }

    /**
     * Envía un correo HTML con la plantilla Cronos.
     */
    public void sendEmail(String recipientEmail, String subject, String message) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new IllegalArgumentException("El destinatario del correo es obligatorio");
        }

        String resolvedSubject = subject != null && !subject.isBlank() ? subject.trim() : "Notificación Cronos";
        String htmlBody = buildGenericHtml(resolvedSubject, message != null && !message.isBlank() ? message : "Sin contenido.");
        sendHtml(recipientEmail.trim(), resolvedSubject, htmlBody);
    }

    public void sendObligationCreated(TaxPayer taxpayer, TaxObligationResponseDTO obligation) {
        if (taxpayer == null || taxpayer.getEmail() == null || taxpayer.getEmail().isBlank()) {
            return;
        }

        String subject = "Nueva obligación registrada — Cronos";
        String html = buildObligationHtml(taxpayer, obligation, "Nueva obligación registrada",
                "Se ha registrado una nueva obligación tributaria en tu cuenta de Cronos.",
                "#3b5bff");
        sendHtml(taxpayer.getEmail(), subject, html);
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
        String html = buildObligationHtml(taxpayer, obligation,
                "Estado actualizado: " + statusLabel,
                "El estado de una obligación tributaria ha sido actualizado en Cronos.",
                accentColor);
        sendHtml(taxpayer.getEmail(), subject, html);
    }

    public void sendReminderEmail(User user, String recipientEmail, String subject, String message) {
        String resolvedSubject = subject != null && !subject.isBlank() ? subject : "Recordatorio — Cronos";
        String resolvedMessage = message != null && !message.isBlank()
                ? message
                : (user != null ? "Recordatorio enviado por " + user.getName() : "Recordatorio Cronos");
        String senderName = user != null && user.getName() != null ? user.getName() : "Equipo Cronos";

        String html = buildReminderHtml(resolvedSubject, resolvedMessage, senderName);
        sendHtml(recipientEmail, resolvedSubject, html);
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

    private String buildGenericHtml(String title, String message) {
        String escapedMessage = escapeHtml(message).replace("\n", "<br/>");
        return wrapInLayout(title,
                "<p style=\"font-size: 15px; color: #94a3b8; line-height: 1.7; margin: 0;\">"
                + escapedMessage + "</p>",
                "#3b5bff");
    }

    private String buildReminderHtml(String title, String message, String senderName) {
        String escapedMessage = escapeHtml(message).replace("\n", "<br/>");
        String body = "<p style=\"font-size: 15px; color: #94a3b8; line-height: 1.7; margin: 0 0 20px;\">"
                + escapedMessage + "</p>"
                + "<table cellpadding=\"0\" cellspacing=\"0\" style=\"margin-top: 8px;\">"
                + "<tr><td style=\"font-size: 12px; color: #64748b; text-transform: uppercase; "
                + "letter-spacing: 0.1em; padding-bottom: 4px;\">Enviado por</td></tr>"
                + "<tr><td style=\"font-size: 15px; color: #e5e7eb; font-weight: 600;\">"
                + escapeHtml(senderName) + "</td></tr></table>";
        return wrapInLayout(title, body, "#4f8cff");
    }

    private String buildObligationHtml(TaxPayer taxpayer, TaxObligationResponseDTO obligation,
                                       String headline, String intro, String accentColor) {
        String statusLabel = switch (obligation.status()) {
            case PENDING -> "Pendiente";
            case IN_PROGRESS -> "En progreso";
            case COMPLETED -> "Completado";
            case OVERDUE -> "Vencido";
            case CANCELLED -> "Cancelado";
        };

        String statusColor = switch (obligation.status()) {
            case COMPLETED -> "#22c55e";
            case OVERDUE -> "#ef4444";
            case IN_PROGRESS -> "#4f8cff";
            case PENDING -> "#f59e0b";
            case CANCELLED -> "#94a3b8";
        };

        String body = "<p style=\"font-size: 15px; color: #94a3b8; line-height: 1.7; margin: 0 0 24px;\">"
                + escapeHtml(intro) + "</p>"

                // Detail card
                + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"background: #0f172a; border: 1px solid #243244; border-radius: 14px; overflow: hidden;\">"
                + "<tr><td style=\"padding: 20px 24px;\">"

                + detailRow("Cliente", taxpayer.getBusinessName())
                + detailRow("Identificación", taxpayer.getIdentificacion())
                + detailRow("Tipo", obligation.type())
                + detailRow("Periodo", obligation.fiscalPeriod())
                + detailRow("Año gravable", String.valueOf(obligation.taxYear()))
                + detailRow("Vencimiento", obligation.dueDate() != null ? obligation.dueDate().toString() : "—")

                // Status pill
                + "<table cellpadding=\"0\" cellspacing=\"0\" style=\"margin-top: 16px;\">"
                + "<tr><td style=\"font-size: 12px; color: #64748b; text-transform: uppercase; "
                + "letter-spacing: 0.1em; padding-bottom: 6px;\">Estado</td></tr>"
                + "<tr><td>"
                + "<span style=\"display: inline-block; padding: 6px 16px; border-radius: 999px; "
                + "font-size: 13px; font-weight: 600; color: " + statusColor + "; "
                + "background: " + hexToRgba(statusColor, 0.14) + "; "
                + "border: 1px solid " + hexToRgba(statusColor, 0.24) + ";\">"
                + escapeHtml(statusLabel) + "</span>"
                + "</td></tr></table>"

                + "</td></tr></table>";

        return wrapInLayout(headline, body, accentColor);
    }

    private String detailRow(String label, String value) {
        return "<table cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom: 12px; width: 100%;\">"
                + "<tr>"
                + "<td style=\"font-size: 12px; color: #64748b; text-transform: uppercase; "
                + "letter-spacing: 0.1em; width: 140px; vertical-align: top; padding-top: 2px;\">"
                + escapeHtml(label) + "</td>"
                + "<td style=\"font-size: 15px; color: #e5e7eb; font-weight: 500;\">"
                + escapeHtml(value != null ? value : "—") + "</td>"
                + "</tr></table>";
    }

    /**
     * Envoltorio principal del correo con la identidad visual Cronos.
     */
    private String wrapInLayout(String title, String bodyContent, String accentColor) {
        return "<!DOCTYPE html>"
                + "<html lang=\"es\">"
                + "<head><meta charset=\"UTF-8\"/>"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/>"
                + "<meta name=\"color-scheme\" content=\"dark\"/>"
                + "<meta name=\"supported-color-schemes\" content=\"dark\"/>"
                + "<title>" + escapeHtml(title) + "</title>"
                + "<style>"
                + "body { margin: 0; padding: 0; background-color: #0b1220; "
                + "-webkit-font-smoothing: antialiased; -moz-osx-font-smoothing: grayscale; }"
                + "img { border: 0; max-width: 100%; }"
                + "</style></head>"
                + "<body style=\"margin: 0; padding: 0; background-color: #0b1220; "
                + "font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;\">"

                // Outer table
                + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"background-color: #0b1220; padding: 40px 20px;\">"
                + "<tr><td align=\"center\">"

                // Container
                + "<table width=\"600\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"max-width: 600px; width: 100%;\">"

                // ── Logo / Brand bar ──
                + "<tr><td style=\"padding: 0 0 32px;\">"
                + "<table cellpadding=\"0\" cellspacing=\"0\"><tr>"
                + "<td style=\"width: 42px; height: 42px; background: linear-gradient(135deg, "
                + accentColor + ", #818cf8); border-radius: 12px; text-align: center; "
                + "vertical-align: middle; font-family: 'Space Grotesk', 'Inter', sans-serif; "
                + "font-size: 18px; font-weight: 700; color: #ffffff;\">C</td>"
                + "<td style=\"padding-left: 14px;\">"
                + "<span style=\"font-family: 'Space Grotesk', 'Inter', sans-serif; "
                + "font-size: 20px; font-weight: 700; color: #e5e7eb; letter-spacing: -0.03em;\">Cronos</span>"
                + "<br/><span style=\"font-size: 12px; color: #64748b; text-transform: uppercase; "
                + "letter-spacing: 0.1em;\">Panel fiscal</span>"
                + "</td></tr></table>"
                + "</td></tr>"

                // ── Main card ──
                + "<tr><td>"
                + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"background: #111827; border: 1px solid #243244; border-radius: 18px; overflow: hidden;\">"

                // Accent strip
                + "<tr><td style=\"height: 4px; background: linear-gradient(90deg, "
                + accentColor + ", #818cf8); font-size: 0; line-height: 0;\">&nbsp;</td></tr>"

                // Card content
                + "<tr><td style=\"padding: 36px 32px 28px;\">"

                // Title
                + "<h1 style=\"margin: 0 0 20px; font-family: 'Space Grotesk', 'Inter', sans-serif; "
                + "font-size: 22px; font-weight: 700; color: #e5e7eb; letter-spacing: -0.03em; "
                + "line-height: 1.3;\">" + escapeHtml(title) + "</h1>"

                // Divider
                + "<hr style=\"border: none; border-top: 1px solid #243244; margin: 0 0 24px;\"/>"

                // Body
                + bodyContent

                + "</td></tr>"

                // Footer inside card
                + "<tr><td style=\"padding: 20px 32px; background: #0f172a; "
                + "border-top: 1px solid #1e293b;\">"
                + "<p style=\"margin: 0; font-size: 12px; color: #64748b; line-height: 1.6;\">"
                + "Ingresa a <strong style=\"color: #94a3b8;\">Cronos</strong> para gestionar "
                + "tus obligaciones y hacer seguimiento en tiempo real."
                + "</p></td></tr>"

                + "</table></td></tr>"

                // ── External footer ──
                + "<tr><td style=\"padding: 28px 0 0; text-align: center;\">"
                + "<p style=\"margin: 0 0 4px; font-size: 11px; color: #475569;\">"
                + "Este correo fue enviado automáticamente por Cronos · Panel Fiscal</p>"
                + "<p style=\"margin: 0; font-size: 11px; color: #334155;\">"
                + "© 2026 Cronos. Gestión Tributaria Inteligente.</p>"
                + "</td></tr>"

                + "</table>"
                + "</td></tr></table>"
                + "</body></html>";
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    private static String hexToRgba(String hex, double alpha) {
        hex = hex.replace("#", "");
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        return "rgba(" + r + ", " + g + ", " + b + ", " + alpha + ")";
    }
}