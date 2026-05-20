package com.cronos.gestiontributaria.common.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    
    private final JavaMailSender emailSender;

    // By making JavaMailSender optional via Spring Boot's conditions, 
    // we can avoid crashes if SMTP is not configured. But standard @Autowired 
    // might fail if the bean doesn't exist. Spring Boot Mail Starter creates it
    // if any property is present, or we can just inject it with @Autowired(required = false)
    public EmailService(@org.springframework.beans.factory.annotation.Autowired(required = false) JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void sendTemporaryPassword(String to, String temporaryPassword) {
        String subject = "Bienvenido a ProWeb - Tus credenciales de acceso";
        String text = "Hola,\n\n"
                + "Se ha creado una cuenta para ti en el portal de ProWeb.\n"
                + "Tu usuario es tu correo electrónico (" + to + ").\n"
                + "Tu contraseña temporal es: " + temporaryPassword + "\n\n"
                + "Por favor, inicia sesión y cambia tu contraseña lo antes posible.\n\n"
                + "Saludos,\nEl equipo de Cronos";

        if (emailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                // message.setFrom("noreply@cronos.com"); // optional
                message.setTo(to);
                message.setSubject(subject);
                message.setText(text);
                emailSender.send(message);
                log.info("Email enviado exitosamente a {}", to);
            } catch (Exception e) {
                log.error("Error al enviar el correo a {}: {}", to, e.getMessage());
                // Fallback a consola
                log.info("FALLBACK EMAIL CONTENT:\nSubject: {}\nTo: {}\n{}", subject, to, text);
            }
        } else {
            // Fallback: Si no hay servidor SMTP configurado (muy común en local dev)
            log.warn("JavaMailSender no está configurado. Imprimiendo el correo en consola para desarrollo:");
            log.info("\n=== INICIO DE CORREO ===\nPara: {}\nAsunto: {}\n\n{}\n=== FIN DE CORREO ===", to, subject, text);
        }
    }
    public void sendObligationReminder(String to, String roleDescription, com.cronos.gestiontributaria.obligaciones.dto.TaxObligationResponseDTO obligation) {
        String subject = "Recordatorio de Obligación: " + obligation.taxPayerName();
        String dueDateStr = obligation.dueDate() != null ? obligation.dueDate().toString() : "No definida";
        String typeStr = obligation.type() != null ? obligation.type().getDescription() : "Desconocido";

        String text = "Hola,\n\n"
                + "Este es un recordatorio para la siguiente obligación tributaria asignada a ti como " + roleDescription + ":\n\n"
                + "- Cliente: " + obligation.taxPayerName() + " (" + obligation.taxPayerIdentificacion() + ")\n"
                + "- Obligación: " + typeStr + "\n"
                + "- Periodo Fiscal: " + obligation.fiscalPeriod() + "\n"
                + "- Vencimiento: " + dueDateStr + "\n"
                + "- Estado Actual: " + obligation.status().name() + "\n\n"
                + "Por favor ingresa al sistema ProWeb para gestionar esta obligación lo antes posible.\n\n"
                + "Saludos,\nEl equipo de Cronos";

        if (emailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(to);
                message.setSubject(subject);
                message.setText(text);
                emailSender.send(message);
                log.info("Email de recordatorio enviado a {}", to);
            } catch (Exception e) {
                log.error("Error al enviar recordatorio a {}: {}", to, e.getMessage());
                log.info("FALLBACK EMAIL CONTENT:\nSubject: {}\nTo: {}\n{}", subject, to, text);
            }
        } else {
            log.warn("JavaMailSender no configurado. Imprimiendo recordatorio en consola:");
            log.info("\n=== INICIO DE CORREO ===\nPara: {}\nAsunto: {}\n\n{}\n=== FIN DE CORREO ===", to, subject, text);
        }
    }
}
