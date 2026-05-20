package com.cronos.gestiontributaria.notification.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cronos.gestiontributaria.notification.model.Notification;
import com.cronos.gestiontributaria.auth.model.User;
import com.cronos.gestiontributaria.auth.service.UserService;
import com.cronos.gestiontributaria.notification.service.NotificationCenterService;
import com.cronos.gestiontributaria.obligaciones.dto.TaxObligationResponseDTO;
import com.cronos.gestiontributaria.obligaciones.service.TaxObligationService;
import com.cronos.gestiontributaria.common.service.EmailService;

@Controller
@RequestMapping("/notificaciones")
@PreAuthorize("hasAnyRole('GERENTE','ASESOR','CONTADOR','AUXILIAR_CONTADOR')")
/**
 * Documentación de la entidad NotificationViewController.
 */
public class NotificationViewController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofPattern("dd MMM yyyy, HH:mm", new Locale("es", "CO"))
            .withLocale(new Locale("es", "CO"));

    private final UserService userService;
    private final NotificationCenterService notificationCenterService;
    private final TaxObligationService obligationService;
    private final EmailService emailService;

    public NotificationViewController(UserService userService,
                                      NotificationCenterService notificationCenterService,
                                      TaxObligationService obligationService,
                                      EmailService emailService) {
        this.userService = userService;
        this.notificationCenterService = notificationCenterService;
        this.obligationService = obligationService;
        this.emailService = emailService;
    }

    @GetMapping
    public String index(Model model, Authentication authentication) {
        User user = notificationCenterService.refreshInbox(loadCurrentUser(authentication));
        List<Notification> notifications = sortedNotifications(user.getNotifications());
        long unreadCount = notifications.stream().filter(notification -> !notification.isRead()).count();
        long urgentCount = notifications.stream().filter(notification -> "danger".equals(notification.getType())).count();
        long upcomingCount = notifications.stream().filter(notification -> "warning".equals(notification.getType())).count();
        long readCount = notifications.stream().filter(Notification::isRead).count();
        String lastSync = notifications.stream()
                .map(Notification::getCreatedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .map(dateTime -> dateTime.format(DATE_FORMATTER))
                .orElse("Sin sincronizar");
        String successMessage = (String) model.asMap().get("successMessage");
        String errorMessage = (String) model.asMap().get("errorMessage");

        model.addAttribute("usuario", user);
        model.addAttribute("notificaciones", notifications);
        model.addAttribute("notificacionesNoLeidas", unreadCount);
        model.addAttribute("notificacionesUrgentes", urgentCount);
        model.addAttribute("notificacionesProximas", upcomingCount);
        model.addAttribute("notificacionesLeidas", readCount);
        model.addAttribute("ultimaSincronizacion", lastSync);
        model.addAttribute("alcanceBandeja", notificationCenterService.describeInboxScope(user));
        if (successMessage != null && !successMessage.isBlank()) {
            model.addAttribute("successMessage", successMessage);
        }
        if (errorMessage != null && !errorMessage.isBlank()) {
            model.addAttribute("errorMessage", errorMessage);
        }
        return "notificaciones/list";
    }

    @PostMapping("/refrescar")
    public String refresh(Authentication authentication, RedirectAttributes redirectAttributes) {
        User user = loadCurrentUser(authentication);
        notificationCenterService.refreshInbox(user);
        redirectAttributes.addFlashAttribute("successMessage", "La bandeja se sincronizó con las obligaciones activas.");
        return "redirect:/notificaciones";
    }

    @PostMapping("/{id}/leer")
    public String markAsRead(@PathVariable String id,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        User user = loadCurrentUser(authentication);
        try {
            notificationCenterService.markAsRead(user, id);
            redirectAttributes.addFlashAttribute("successMessage", "Notificación marcada como leída.");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/notificaciones";
    }

    @PostMapping("/{id}/eliminar")
    public String delete(@PathVariable String id,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        User user = loadCurrentUser(authentication);
        try {
            notificationCenterService.deleteNotification(user, id);
            redirectAttributes.addFlashAttribute("successMessage", "Notificación eliminada.");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/notificaciones";
    }

    @PostMapping("/marcar-todas")
    public String markAllAsRead(Authentication authentication, RedirectAttributes redirectAttributes) {
        User user = loadCurrentUser(authentication);
        notificationCenterService.markAllAsRead(user);
        redirectAttributes.addFlashAttribute("successMessage", "Todas las notificaciones quedaron leídas.");
        return "redirect:/notificaciones";
    }

    @PostMapping("/limpiar-leidas")
    public String clearRead(Authentication authentication, RedirectAttributes redirectAttributes) {
        User user = loadCurrentUser(authentication);
        notificationCenterService.clearReadNotifications(user);
        redirectAttributes.addFlashAttribute("successMessage", "Se limpiaron las notificaciones leídas.");
        return "redirect:/notificaciones";
    }

    @PostMapping("/{id}/notificar")
    @PreAuthorize("hasAnyRole('GERENTE', 'ASESOR', 'ADMIN')")
    public String notifySingle(@PathVariable String id, RedirectAttributes redirectAttrs) {
        try {
            TaxObligationResponseDTO obligacion = obligationService.findById(id);
            int sent = sendNotificationEmailsForObligation(obligacion);
            if (sent > 0) {
                redirectAttrs.addFlashAttribute("successMessage", "Se envió notificación a " + sent + " responsable(s) de la obligación.");
            } else {
                redirectAttrs.addFlashAttribute("warningMessage", "No hay responsables con correo asignado para notificar.");
            }
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage", "Error al intentar notificar: " + e.getMessage());
        }
        return "redirect:/notificaciones";
    }

    @PostMapping("/notificar-masivo")
    @PreAuthorize("hasAnyRole('GERENTE', 'ASESOR', 'ADMIN')")
    public String notifyMassive(Authentication authentication, RedirectAttributes redirectAttrs) {
        // Obtenemos las notificaciones activas del usuario actual
        User user = loadCurrentUser(authentication);
        List<Notification> notificaciones = user.getNotifications();
        
        int totalSent = 0;
        if (notificaciones != null) {
            for (Notification n : notificaciones) {
                if (n.getSourceId() != null && !n.getSourceId().isBlank()) {
                    try {
                        TaxObligationResponseDTO obligacion = obligationService.findById(n.getSourceId());
                        totalSent += sendNotificationEmailsForObligation(obligacion);
                    } catch (Exception ignored) {
                        // Ignorar si la obligación ya no existe
                    }
                }
            }
        }

        if (totalSent > 0) {
            redirectAttrs.addFlashAttribute("successMessage", "Se enviaron " + totalSent + " notificaciones de forma masiva basadas en tu bandeja.");
        } else {
            redirectAttrs.addFlashAttribute("warningMessage", "No se encontraron responsables para notificar en las obligaciones actuales de tu bandeja.");
        }
        return "redirect:/notificaciones";
    }

    private int sendNotificationEmailsForObligation(TaxObligationResponseDTO obligacion) {
        int count = 0;
        if (obligacion.counterResponsible() != null && obligacion.counterResponsible().userEmail() != null && !obligacion.counterResponsible().userEmail().isBlank()) {
            emailService.sendObligationReminder(obligacion.counterResponsible().userEmail(), "Contador", obligacion);
            count++;
        }
        if (obligacion.auxiliaryResponsible() != null && obligacion.auxiliaryResponsible().userEmail() != null && !obligacion.auxiliaryResponsible().userEmail().isBlank()) {
            emailService.sendObligationReminder(obligacion.auxiliaryResponsible().userEmail(), "Auxiliar Contable", obligacion);
            count++;
        }
        return count;
    }

    private User loadCurrentUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private List<Notification> sortedNotifications(List<Notification> notifications) {
        if (notifications == null) {
            return List.of();
        }
        return notifications.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Notification::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }
}