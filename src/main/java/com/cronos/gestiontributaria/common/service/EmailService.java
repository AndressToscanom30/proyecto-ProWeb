package com.cronos.gestiontributaria.common.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    
    private final JavaMailSender emailSender;
    private final TemplateEngine templateEngine;

    // By making JavaMailSender optional via Spring Boot's conditions, 
    // we can avoid crashes if SMTP is not configured. But standard @Autowired 
    // might fail if the bean doesn't exist. Spring Boot Mail Starter creates it
    // if any property is present, or we can just inject it with @Autowired(required = false)
    public EmailService(@org.springframework.beans.factory.annotation.Autowired(required = false) JavaMailSender emailSender,
                        TemplateEngine templateEngine) {
        this.emailSender = emailSender;
        this.templateEngine = templateEngine;
    }

    public void sendTemporaryPassword(String to, String temporaryPassword) {
        String subject = "Bienvenido a Cronos - Tus credenciales de acceso";
        
        Context context = new Context();
        context.setVariable("email", to);
        context.setVariable("temporaryPassword", temporaryPassword);
        
        String htmlContent = templateEngine.process("email/temporary-password", context);

        if (emailSender != null) {
            try {
                MimeMessage message = emailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(htmlContent, true); // true indicates HTML
                
                emailSender.send(message);
                log.info("Email HTML enviado exitosamente a {}", to);
            } catch (Exception e) {
                log.error("Error al enviar el correo a {}: {}", to, e.getMessage());
                log.info("FALLBACK EMAIL CONTENT:\nSubject: {}\nTo: {}\nHTML content generated.", subject, to);
            }
        } else {
            // Fallback: Si no hay servidor SMTP configurado
            log.warn("JavaMailSender no está configurado. Imprimiendo el correo en consola para desarrollo:");
            log.info("\n=== INICIO DE CORREO HTML ===\nPara: {}\nAsunto: {}\n\n{}\n=== FIN DE CORREO ===", to, subject, htmlContent);
        }
    }
    public void sendObligationReminder(String to, String roleDescription, com.cronos.gestiontributaria.obligaciones.dto.TaxObligationResponseDTO obligation) {
        String subject = "Recordatorio de Obligación: " + obligation.taxPayerName();
        
        Context context = new Context();
        context.setVariable("roleDescription", roleDescription);
        context.setVariable("obligation", obligation);
        
        String htmlContent = templateEngine.process("email/obligation-reminder", context);

        if (emailSender != null) {
            try {
                MimeMessage message = emailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(htmlContent, true); // true indicates HTML
                
                emailSender.send(message);
                log.info("Email HTML de recordatorio enviado a {}", to);
            } catch (Exception e) {
                log.error("Error al enviar recordatorio a {}: {}", to, e.getMessage());
                log.info("FALLBACK EMAIL CONTENT:\nSubject: {}\nTo: {}\nHTML content generated.", subject, to);
            }
        } else {
            log.warn("JavaMailSender no configurado. Imprimiendo recordatorio HTML en consola:");
            log.info("\n=== INICIO DE CORREO HTML ===\nPara: {}\nAsunto: {}\n\n{}\n=== FIN DE CORREO ===", to, subject, htmlContent);
        }
    }
}
