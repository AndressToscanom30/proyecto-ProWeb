package com.cronos.gestiontributaria.notification.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.cronos.gestiontributaria.auth.model.User;
import com.cronos.gestiontributaria.auth.repository.UserRepository;
import com.cronos.gestiontributaria.common.TaxObligationStatus;
import com.cronos.gestiontributaria.notification.model.Notification;
import com.cronos.gestiontributaria.obligaciones.dto.TaxObligationResponseDTO;
import com.cronos.gestiontributaria.obligaciones.service.TaxObligationService;

/**
 * Mantiene la bandeja interna de notificaciones sincronizada con las
 * obligaciones reales del sistema.
 */
@Service
public class NotificationCenterService {

    private static final int ALERT_WINDOW_DAYS = 14;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(new Locale("es", "CO"));

    private final UserRepository userRepository;
    private final TaxObligationService obligationService;

    public NotificationCenterService(UserRepository userRepository,
                                     TaxObligationService obligationService) {
        this.userRepository = userRepository;
        this.obligationService = obligationService;
    }

    public User refreshInbox(User user) {
        User persisted = requireUser(user);
        List<Notification> existing = safeNotifications(persisted);
        List<Notification> generated = buildSystemNotifications(persisted);
        List<Notification> merged = mergeNotifications(existing, generated);
        persisted.setNotifications(merged);
        return userRepository.save(persisted);
    }

    public User markAsRead(User user, String notificationId) {
        User persisted = requireUser(user);
        boolean updated = false;
        for (Notification notification : safeNotifications(persisted)) {
            if (matchesNotification(notification, notificationId)) {
                notification.markAsRead();
                updated = true;
                break;
            }
        }

        if (!updated) {
            throw new NoSuchElementException("Notificación no encontrada con ID: " + notificationId);
        }

        persisted.setNotifications(sortedCopy(persisted.getNotifications()));
        return userRepository.save(persisted);
    }

    public User markAllAsRead(User user) {
        User persisted = requireUser(user);
        for (Notification notification : safeNotifications(persisted)) {
            notification.markAsRead();
        }
        persisted.setNotifications(sortedCopy(persisted.getNotifications()));
        return userRepository.save(persisted);
    }

    public User clearReadNotifications(User user) {
        User persisted = requireUser(user);
        List<Notification> remaining = safeNotifications(persisted).stream()
                .filter(notification -> !notification.isRead())
                .toList();
        persisted.setNotifications(new ArrayList<>(remaining));
        return userRepository.save(persisted);
    }

    public User deleteNotification(User user, String notificationId) {
        User persisted = requireUser(user);
        List<Notification> remaining = safeNotifications(persisted).stream()
                .filter(notification -> !matchesNotification(notification, notificationId))
                .toList();

        if (remaining.size() == safeNotifications(persisted).size()) {
            throw new NoSuchElementException("Notificación no encontrada con ID: " + notificationId);
        }

        persisted.setNotifications(new ArrayList<>(remaining));
        return userRepository.save(persisted);
    }

    public String describeInboxScope(User user) {
        String roleName = normalizedRoleName(user);
        return switch (roleName) {
            case "ROLE_CONTADOR" -> "Obligaciones asignadas";
            case "ROLE_AUXILIAR_CONTADOR" -> "Apoyo operativo";
            case "ROLE_GERENTE", "ROLE_ASESOR", "ROLE_ADMIN" -> "Cartera completa";
            default -> "Bandeja interna";
        };
    }

    private User requireUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Usuario no disponible");
        }

        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            return userRepository.findByEmail(normalizeEmail(user.getEmail()))
                    .orElseThrow(() -> new NoSuchElementException(
                            "Usuario no encontrado con correo: " + user.getEmail()));
        }

        if (user.getId() != null && !user.getId().isBlank()) {
            return userRepository.findById(user.getId())
                    .orElseThrow(() -> new NoSuchElementException(
                            "Usuario no encontrado con ID: " + user.getId()));
        }

        throw new IllegalArgumentException("Usuario no disponible");
    }

    private List<Notification> buildSystemNotifications(User user) {
        List<TaxObligationResponseDTO> obligations = loadRelevantObligations(user);
        LocalDate today = LocalDate.now();
        LocalDate windowEnd = today.plusDays(ALERT_WINDOW_DAYS);

        return obligations.stream()
                .filter(Objects::nonNull)
                .filter(this::isRelevant)
                .map(obligation -> toNotification(obligation, today, windowEnd))
                .sorted(Comparator.comparing(Notification::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private List<TaxObligationResponseDTO> loadRelevantObligations(User user) {
        String roleName = normalizedRoleName(user);
        return switch (roleName) {
            case "ROLE_CONTADOR" -> obligationService.findByCounterResponsibleUserId(user.getId());
            case "ROLE_AUXILIAR_CONTADOR" -> obligationService.findByAuxiliaryResponsibleUserId(user.getId());
            case "ROLE_GERENTE", "ROLE_ASESOR", "ROLE_ADMIN" -> obligationService.findAll();
            default -> List.of();
        };
    }

    private boolean isRelevant(TaxObligationResponseDTO obligation) {
        if (obligation.dueDate() == null) {
            return false;
        }
        return obligation.status() == TaxObligationStatus.PENDING
                || obligation.status() == TaxObligationStatus.IN_PROGRESS
                || obligation.status() == TaxObligationStatus.OVERDUE;
    }

    private Notification toNotification(TaxObligationResponseDTO obligation, LocalDate today, LocalDate windowEnd) {
        LocalDate dueDate = obligation.dueDate();
        long daysUntilDue = ChronoUnit.DAYS.between(today, dueDate);
        boolean overdue = dueDate.isBefore(today);
        boolean dueSoon = !overdue && !dueDate.isAfter(windowEnd);

        String tone = overdue ? "danger" : dueSoon ? "warning" : "primary";
        String contextLabel = buildContextLabel(obligation);
        String title;
        String message;

        if (overdue) {
            title = "Obligación vencida";
            message = "Tienes pendiente la revisión de " + contextLabel
                + ". Venció el " + dueDate.format(DATE_FORMATTER) + ".";
        } else if (daysUntilDue == 0) {
            title = "Vence hoy";
            message = "Revisa " + contextLabel + ". Vence hoy.";
        } else if (dueSoon) {
            title = "Vence pronto";
            message = "Tienes una obligación próxima: " + contextLabel
                + " vence en " + daysUntilDue + " días.";
        } else {
            title = "Seguimiento interno";
            message = "Mantén seguimiento a " + contextLabel + ".";
        }

        return new Notification(
            title,
                message,
                tone,
                false,
                LocalDateTime.now(),
                obligation.id(),
            obligation.id() != null ? "/obligaciones/" + obligation.id() : null,
            contextLabel);
    }

        private String buildContextLabel(TaxObligationResponseDTO obligation) {
        String client = obligation.taxPayerName() != null && !obligation.taxPayerName().isBlank()
            ? obligation.taxPayerName()
            : "Contribuyente";
        String type = obligation.type() != null ? obligation.type().getDescription() : "Obligación";
        String period = obligation.fiscalPeriod() != null ? obligation.fiscalPeriod() : "sin periodo";
        return client + " · " + type + " · " + period;
        }

    private List<Notification> mergeNotifications(List<Notification> existing, List<Notification> generated) {
        Map<String, Notification> generatedBySource = generated.stream()
                .filter(notification -> notification.getSourceId() != null && !notification.getSourceId().isBlank())
                .collect(Collectors.toMap(Notification::getSourceId, notification -> notification,
                        (left, right) -> left, LinkedHashMap::new));

        Map<String, Notification> merged = new LinkedHashMap<>();

        for (Notification notification : existing) {
            if (notification == null) {
                continue;
            }
            if (notification.getSourceId() != null && !notification.getSourceId().isBlank()) {
                Notification replacement = generatedBySource.get(notification.getSourceId());
                if (replacement != null) {
                    replacement.setId(notification.getId());
                    replacement.setRead(notification.isRead());
                    replacement.setCreatedAt(notification.getCreatedAt() != null
                            ? notification.getCreatedAt()
                            : replacement.getCreatedAt());
                    merged.put(replacement.getId(), replacement);
                    continue;
                }
            }
            merged.put(notification.getId(), notification);
        }

        for (Notification notification : generated) {
            if (notification.getSourceId() == null || notification.getSourceId().isBlank()) {
                merged.put(notification.getId(), notification);
                continue;
            }
            boolean alreadyPresent = merged.values().stream()
                    .anyMatch(existingNotification -> notification.getSourceId().equals(existingNotification.getSourceId()));
            if (!alreadyPresent) {
                merged.put(notification.getId(), notification);
            }
        }

        List<Notification> result = new ArrayList<>(merged.values());
        result.sort(Comparator.comparing(Notification::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    private List<Notification> safeNotifications(User user) {
        if (user.getNotifications() == null) {
            user.setNotifications(new ArrayList<>());
        }
        return user.getNotifications().stream()
                .filter(Objects::nonNull)
                .map(this::normalizeNotification)
                .toList();
    }

    private List<Notification> sortedCopy(List<Notification> notifications) {
        return notifications == null ? new ArrayList<>() : notifications.stream()
                .filter(Objects::nonNull)
                .map(this::normalizeNotification)
                .sorted(Comparator.comparing(Notification::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private boolean matchesNotification(Notification notification, String notificationId) {
        if (notification == null || notificationId == null || notificationId.isBlank()) {
            return false;
        }
        return notificationId.equals(notification.getId())
                || notificationId.equals(notification.getSourceId());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizedRoleName(User user) {
        if (user == null || user.getRole() == null || user.getRole().getName() == null) {
            return "";
        }
        return user.getRole().getName().trim().toUpperCase(Locale.ROOT);
    }

    private Notification normalizeNotification(Notification notification) {
        if (notification.getId() == null || notification.getId().isBlank()) {
            notification.setId(UUID.randomUUID().toString());
        }
        if (notification.getCreatedAt() == null) {
            notification.setCreatedAt(LocalDateTime.now());
        }
        return notification;
    }
}