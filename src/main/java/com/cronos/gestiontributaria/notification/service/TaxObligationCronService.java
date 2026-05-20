package com.cronos.gestiontributaria.notification.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.cronos.gestiontributaria.common.TaxObligationStatus;
import com.cronos.gestiontributaria.common.service.EmailService;
import com.cronos.gestiontributaria.obligaciones.dto.TaxObligationResponseDTO;
import com.cronos.gestiontributaria.obligaciones.service.TaxObligationService;

/**
 * Servicio encargado de ejecutar tareas programadas (Cron Jobs)
 * para notificar a los responsables sobre vencimientos de obligaciones.
 */
@Service
public class TaxObligationCronService {

    private static final Logger log = LoggerFactory.getLogger(TaxObligationCronService.class);

    private final TaxObligationService taxObligationService;
    private final EmailService emailService;

    public TaxObligationCronService(TaxObligationService taxObligationService, EmailService emailService) {
        this.taxObligationService = taxObligationService;
        this.emailService = emailService;
    }

    /**
     * Se ejecuta todos los días a las 08:00 AM.
     * Busca obligaciones pendientes o en progreso que venzan hoy, mañana, o en 3 días,
     * y envía correos automáticos a los responsables asignados.
     */
    @Scheduled(cron = "${cronos.alerts.cron:0 0 8 * * ?}")
    public void alertUpcomingExpirations() {
        log.info("Iniciando tarea programada: Verificación de vencimientos automáticos.");
        LocalDate today = LocalDate.now();
        List<TaxObligationResponseDTO> obligations = taxObligationService.findAll();

        int sentEmailsCount = 0;

        for (TaxObligationResponseDTO obligation : obligations) {
            if (!isRelevantStatus(obligation.status()) || obligation.dueDate() == null) {
                continue;
            }

            long daysUntilDue = ChronoUnit.DAYS.between(today, obligation.dueDate());

            // Filtramos específicamente para vencimientos a 0, 1 y 3 días.
            // Si quieres enviar correos para obligaciones ya vencidas (daysUntilDue < 0), se podría agregar.
            if (daysUntilDue == 0 || daysUntilDue == 1 || daysUntilDue == 3) {
                sentEmailsCount += sendReminders(obligation);
            }
        }

        log.info("Tarea programada finalizada. Se enviaron {} notificaciones de vencimiento.", sentEmailsCount);
    }

    private boolean isRelevantStatus(TaxObligationStatus status) {
        return status == TaxObligationStatus.PENDING || status == TaxObligationStatus.IN_PROGRESS;
    }

    private int sendReminders(TaxObligationResponseDTO obligation) {
        int count = 0;
        if (obligation.counterResponsible() != null && obligation.counterResponsible().userEmail() != null && !obligation.counterResponsible().userEmail().isBlank()) {
            emailService.sendObligationReminder(obligation.counterResponsible().userEmail(), "Contador", obligation);
            count++;
        }
        if (obligation.auxiliaryResponsible() != null && obligation.auxiliaryResponsible().userEmail() != null && !obligation.auxiliaryResponsible().userEmail().isBlank()) {
            emailService.sendObligationReminder(obligation.auxiliaryResponsible().userEmail(), "Auxiliar Contable", obligation);
            count++;
        }
        return count;
    }
}
