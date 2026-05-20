package com.cronos.gestiontributaria.notification.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.util.List;

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
    private final String appBaseUrl;

    public NotificationMailService(JavaMailSender mailSender,
                                   @Value("${spring.mail.username:}") String mailUsername,
                                   @Value("${app.mail.from:}") String fromAddress,
                                   @Value("${app.base-url:http://localhost:8080}") String appBaseUrl) {
        this.mailSender = mailSender;
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
        String htmlBody = buildStructuredEmailHtml(
                resolvedSubject,
                resolvedSubject,
                message != null && !message.isBlank() ? message : "Sin contenido.",
                List.of(),
                null,
                null,
                "Mensaje enviado automáticamente por Cronos.");
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
        String htmlBody = buildStructuredEmailHtml(
                headline != null && !headline.isBlank() ? headline : resolvedSubject,
                resolvedSubject,
                intro,
                details != null ? details : List.of(),
                actionLabel,
                actionPath,
                "Mensaje enviado automáticamente por Cronos.");
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

    /**
     * Envoltorio principal del correo con la identidad visual Cronos.
     */
    private String buildStructuredEmailHtml(String headline, String title, String intro,
                                            List<NotificationEmailDetail> details, String actionLabel,
                                            String actionPath, String footerNote) {
        String bodyContent = buildBodyContent(intro, details, actionLabel, actionPath, footerNote);
        return wrapInLayout(headline, bodyContent, "#2563eb");
    }

    private String buildBodyContent(String intro, List<NotificationEmailDetail> details, String actionLabel,
                                    String actionPath, String footerNote) {
        StringBuilder body = new StringBuilder();
        if (intro != null && !intro.isBlank()) {
            body.append("<p style=\"margin: 0 0 20px; font-size: 15px; line-height: 1.7; color: #475569;\">")
                    .append(escapeHtml(intro).replace("\n", "<br/>")).append("</p>");
        }

        if (details != null && !details.isEmpty()) {
            body.append("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                    + "style=\"border: 1px solid #dbe4f0; border-radius: 14px; overflow: hidden; margin: 0 0 24px;\">")
                    .append("<tr><td style=\"padding: 18px 20px; background: #f8fbff;\">");
            for (NotificationEmailDetail detail : details) {
                body.append(detailRow(detail.label(), detail.value()));
            }
            body.append("</td></tr></table>");
        }

        if (actionPath != null && !actionPath.isBlank()) {
            body.append("<table cellpadding=\"0\" cellspacing=\"0\" style=\"margin: 0 0 22px;\"><tr><td>")
                    .append("<a href=\"").append(escapeHtml(buildAbsoluteUrl(actionPath))).append("\" style=\"")
                    .append("display: inline-block; padding: 12px 20px; border-radius: 999px; ")
                    .append("background: #2563eb; color: #ffffff; text-decoration: none; font-size: 14px; font-weight: 700; ")
                    .append("letter-spacing: 0.01em;\">")
                    .append(escapeHtml(actionLabel != null ? actionLabel : "Abrir sistema"))
                    .append("</a></td></tr></table>");
        }

        if (footerNote != null && !footerNote.isBlank()) {
            body.append("<p style=\"margin: 0; font-size: 12px; color: #64748b; line-height: 1.6;\">")
                    .append(escapeHtml(footerNote)).append("</p>");
        }

        return body.toString();
    }

    private String detailRow(String label, String value) {
        return "<table cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom: 12px; width: 100%;\">"
                + "<tr>"
                + "<td style=\"font-size: 12px; color: #64748b; text-transform: uppercase; letter-spacing: 0.1em; width: 160px; vertical-align: top; padding-top: 2px;\">"
                + escapeHtml(label) + "</td>"
                + "<td style=\"font-size: 15px; color: #1e293b; font-weight: 600;\">"
                + escapeHtml(value != null ? value : "—") + "</td>"
                + "</tr></table>";
    }

    private String wrapInLayout(String title, String bodyContent, String accentColor) {
        return "<!DOCTYPE html>"
                + "<html lang=\"es\">"
                + "<head><meta charset=\"UTF-8\"/>"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/>"
                + "<meta name=\"color-scheme\" content=\"light\"/>"
                + "<meta name=\"supported-color-schemes\" content=\"light\"/>"
                + "<title>" + escapeHtml(title) + "</title>"
                + "<style>"
                + "body { margin: 0; padding: 0; background-color: #f4f7fb; "
                + "-webkit-font-smoothing: antialiased; -moz-osx-font-smoothing: grayscale; }"
                + "img { border: 0; max-width: 100%; }"
                + "</style></head>"
                + "<body style=\"margin: 0; padding: 0; background-color: #f4f7fb; "
                + "font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;\">"

                // Outer table
                + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"background-color: #f4f7fb; padding: 36px 18px;\">"
                + "<tr><td align=\"center\">"

                // Container
                + "<table width=\"600\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"max-width: 600px; width: 100%;\">"

                // ── Logo / Brand bar ──
                + "<tr><td style=\"padding: 0 0 32px;\">"
                + "<table cellpadding=\"0\" cellspacing=\"0\"><tr>"
                + "<td style=\"width: 42px; height: 42px; background: linear-gradient(135deg, "
                + accentColor + ", #60a5fa); border-radius: 12px; text-align: center; "
                + "vertical-align: middle; font-family: 'Space Grotesk', 'Inter', sans-serif; "
                + "font-size: 18px; font-weight: 700; color: #ffffff;\">C</td>"
                + "<td style=\"padding-left: 14px;\">"
                + "<span style=\"font-family: 'Space Grotesk', 'Inter', sans-serif; "
                + "font-size: 20px; font-weight: 700; color: #0f172a; letter-spacing: -0.03em;\">Cronos</span>"
                + "<br/><span style=\"font-size: 12px; color: #64748b; text-transform: uppercase; "
                + "letter-spacing: 0.1em;\">Panel fiscal</span>"
                + "</td></tr></table>"
                + "</td></tr>"

                // ── Main card ──
                + "<tr><td>"
                + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"background: #ffffff; border: 1px solid #dbe4f0; border-radius: 18px; overflow: hidden; box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);\">"

                // Accent strip
                + "<tr><td style=\"height: 4px; background: linear-gradient(90deg, "
                + accentColor + ", #818cf8); font-size: 0; line-height: 0;\">&nbsp;</td></tr>"

                // Card content
                + "<tr><td style=\"padding: 36px 32px 28px;\">"

                // Title
                + "<h1 style=\"margin: 0 0 20px; font-family: 'Space Grotesk', 'Inter', sans-serif; "
                + "font-size: 22px; font-weight: 700; color: #0f172a; letter-spacing: -0.03em; "
                + "line-height: 1.3;\">" + escapeHtml(title) + "</h1>"

                // Divider
                + "<hr style=\"border: none; border-top: 1px solid #e2e8f0; margin: 0 0 24px;\"/>"

                // Body
                + bodyContent

                + "</td></tr>"

                // Footer inside card
                + "<tr><td style=\"padding: 20px 32px; background: #f8fbff; border-top: 1px solid #e2e8f0;\">"
                + "<p style=\"margin: 0; font-size: 12px; color: #64748b; line-height: 1.6;\">"
                + "Ingresa a <strong style=\"color: #0f172a;\">Cronos</strong> para gestionar "
                + "tus obligaciones y hacer seguimiento en tiempo real."
                + "</p></td></tr>"

                + "</table></td></tr>"

                // ── External footer ──
                + "<tr><td style=\"padding: 28px 0 0; text-align: center;\">"
                + "<p style=\"margin: 0 0 4px; font-size: 11px; color: #64748b;\">"
                + "Este correo fue enviado automáticamente por Cronos · Panel Fiscal</p>"
                + "<p style=\"margin: 0; font-size: 11px; color: #94a3b8;\">"
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