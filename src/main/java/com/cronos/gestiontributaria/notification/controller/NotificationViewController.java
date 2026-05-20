package com.cronos.gestiontributaria.notification.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.http.HttpStatus;
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

@Controller
@RequestMapping("/notificaciones")
/**
 * Documentación de la entidad NotificationViewController.
 */
public class NotificationViewController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofPattern("dd MMM yyyy, HH:mm", new Locale("es", "CO"))
            .withLocale(new Locale("es", "CO"));

    private final UserService userService;
    private final NotificationCenterService notificationCenterService;

    public NotificationViewController(UserService userService,
                                      NotificationCenterService notificationCenterService) {
        this.userService = userService;
        this.notificationCenterService = notificationCenterService;
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
        model.addAttribute("alcanceBandeja", user.getTaxPayerId() != null && !user.getTaxPayerId().isBlank()
                ? "Solo tu contribuyente vinculado"
                : "Toda la cartera fiscal");
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