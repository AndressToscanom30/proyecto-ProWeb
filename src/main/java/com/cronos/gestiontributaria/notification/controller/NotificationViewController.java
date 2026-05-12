package com.cronos.gestiontributaria.notification.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cronos.gestiontributaria.auth.model.User;
import com.cronos.gestiontributaria.auth.service.UserService;
import com.cronos.gestiontributaria.clientes.model.TaxPayer;
import com.cronos.gestiontributaria.clientes.service.TaxPayerService;
import com.cronos.gestiontributaria.common.TaxObligationStatus;
import com.cronos.gestiontributaria.notification.service.NotificationMailService;
import com.cronos.gestiontributaria.notification.view.NotificationAlert;
import com.cronos.gestiontributaria.obligaciones.dto.TaxObligationResponseDTO;
import com.cronos.gestiontributaria.obligaciones.service.TaxObligationService;

@Controller
@RequestMapping("/notificaciones")
/**
 * Documentación de la entidad NotificationViewController.
 */
public class NotificationViewController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(new Locale("es", "CO"));

    private final UserService userService;
    private final TaxObligationService obligationService;
    private final TaxPayerService taxPayerService;
    private final NotificationMailService notificationMailService;

    public NotificationViewController(UserService userService,
                                      TaxObligationService obligationService,
                                      TaxPayerService taxPayerService,
                                      NotificationMailService notificationMailService) {
        this.userService = userService;
        this.obligationService = obligationService;
        this.taxPayerService = taxPayerService;
        this.notificationMailService = notificationMailService;
    }

    @GetMapping
    public String index(Model model, Authentication authentication) {
        User user = loadCurrentUser(authentication);
        List<NotificationAlert> alerts = buildAlerts();
        long overdueCount = alerts.stream().filter(alert -> "danger".equals(alert.tone())).count();
        long soonCount = alerts.stream().filter(alert -> "warning".equals(alert.tone())).count();
        String successMessage = (String) model.asMap().get("successMessage");
        String errorMessage = (String) model.asMap().get("errorMessage");

        model.addAttribute("usuario", user);
        model.addAttribute("alertas", alerts);
        model.addAttribute("alertasVencidas", overdueCount);
        model.addAttribute("alertasProximas", soonCount);
        model.addAttribute("correoPrueba", user.getEmail());
        if (successMessage != null && !successMessage.isBlank()) {
            model.addAttribute("successMessage", successMessage);
        }
        if (errorMessage != null && !errorMessage.isBlank()) {
            model.addAttribute("errorMessage", errorMessage);
        }
        return "notificaciones/list";
    }

    @PostMapping("/enviar")
    public String sendEmail(@RequestParam String recipientEmail,
                            @RequestParam String subject,
                            @RequestParam String message,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        User user = loadCurrentUser(authentication);

        try {
            notificationMailService.sendReminderEmail(user, recipientEmail, subject, message);
            redirectAttributes.addFlashAttribute("successMessage", "Correo enviado a " + recipientEmail.trim());
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "No se pudo enviar el correo. Revisa la configuración SMTP.");
        }

        return "redirect:/notificaciones";
    }

    private User loadCurrentUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private List<NotificationAlert> buildAlerts() {
        Map<String, TaxPayer> taxpayersById = taxPayerService.findAll().stream()
                .filter(taxpayer -> taxpayer.getId() != null)
                .collect(Collectors.toMap(TaxPayer::getId, taxpayer -> taxpayer, (left, right) -> left));

        LocalDate limitDate = LocalDate.now().plusDays(7);
        return obligationService.findAll().stream()
                .filter(this::isRelevant)
                .map(obligation -> toAlert(obligation, taxpayersById.get(obligation.taxPayerId()), limitDate))
                .filter(Objects::nonNull)
                .toList();
    }

    private boolean isRelevant(TaxObligationResponseDTO obligation) {
        if (obligation.dueDate() == null) {
            return false;
        }
        return obligation.status() == TaxObligationStatus.PENDING
                || obligation.status() == TaxObligationStatus.IN_PROGRESS
                || obligation.status() == TaxObligationStatus.OVERDUE;
    }

    private NotificationAlert toAlert(TaxObligationResponseDTO obligation, TaxPayer taxpayer, LocalDate limitDate) {
        if (taxpayer == null || taxpayer.getEmail() == null || taxpayer.getEmail().isBlank()) {
            return null;
        }

        String dueDateLabel = obligation.dueDate() != null ? obligation.dueDate().format(DATE_FORMATTER) : "Sin fecha";
        String statusLabel = statusLabel(obligation.status());
        String tone = toneFor(obligation, limitDate);
        String obligationLabel = obligation.type() + " · " + obligation.fiscalPeriod();
        String subject = "Recordatorio de obligación " + obligationLabel;
        String body = buildEmailBody(taxpayer, obligation, dueDateLabel, statusLabel);

        return new NotificationAlert(
                taxpayer.getBusinessName(),
                taxpayer.getIdentificacion(),
                taxpayer.getEmail(),
                obligationLabel,
                dueDateLabel,
                statusLabel,
                tone,
                subject,
                body);
    }

    private String statusLabel(TaxObligationStatus status) {
        return switch (status) {
            case PENDING -> "Pendiente";
            case IN_PROGRESS -> "En progreso";
            case COMPLETED -> "Completado";
            case OVERDUE -> "Vencido";
            case CANCELLED -> "Cancelado";
        };
    }

    private String toneFor(TaxObligationResponseDTO obligation, LocalDate limitDate) {
        if (obligation.dueDate().isBefore(LocalDate.now())) {
            return "danger";
        }
        if (!obligation.dueDate().isAfter(limitDate)) {
            return "warning";
        }
        return "primary";
    }

    private String buildEmailBody(TaxPayer taxpayer, TaxObligationResponseDTO obligation, String dueDateLabel,
                                  String statusLabel) {
        StringBuilder builder = new StringBuilder();
        builder.append("Hola ").append(taxpayer.getBusinessName()).append(",\n\n");
        builder.append("Este es un recordatorio de TaxControl.\n\n");
        builder.append("Cliente: ").append(taxpayer.getBusinessName()).append("\n");
        builder.append("Identificación: ").append(taxpayer.getIdentificacion()).append("\n");
        builder.append("Obligación: ").append(obligation.type()).append("\n");
        builder.append("Periodo: ").append(obligation.fiscalPeriod()).append("\n");
        builder.append("Vencimiento: ").append(dueDateLabel).append("\n");
        builder.append("Estado: ").append(statusLabel).append("\n\n");
        builder.append("Ingresa a TaxControl para revisar el detalle y actualizar el seguimiento.");
        return builder.toString();
    }
}