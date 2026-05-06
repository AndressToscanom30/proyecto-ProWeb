package com.cronos.gestiontributaria.calendarios.model;

import com.cronos.gestiontributaria.obligaciones.model.TaxObligation;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class AlertRules {
    private List<Integer> daysInAdvance;
    private boolean active;
    private List<TaxObligation> obligations;

    public AlertRules(List<Integer> daysInAdvance, boolean active, List<TaxObligation> obligations) {
        this.daysInAdvance = daysInAdvance != null ? daysInAdvance : new ArrayList<>();
        this.active = active;
        this.obligations = obligations != null ? obligations : new ArrayList<>();
    }

    public List<Alert> generateAlerts(TaxObligation obligation) {
        List<Alert> result = new ArrayList<>();
        if (!active || obligation == null || obligation.getDueDate() == null) {
            return result;
        }
        for (Integer days : daysInAdvance) {
            if (days == null) {
                continue;
            }
            LocalDate scheduledDate = obligation.getDueDate().minusDays(days);
            LocalDateTime scheduledAt = LocalDateTime.of(scheduledDate, LocalTime.MIDNIGHT);
            Alert alert = new Alert(days, false, scheduledAt, null);
            result.add(alert);
            if (obligation.getAlerts() != null) {
                obligation.getAlerts().add(alert);
            }
        }
        return result;
    }

    public List<Integer> getDaysInAdvance() {
        return daysInAdvance;
    }

    public void setDaysInAdvance(List<Integer> daysInAdvance) {
        this.daysInAdvance = daysInAdvance != null ? daysInAdvance : new ArrayList<>();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<TaxObligation> getObligations() {
        return obligations;
    }

    public void setObligations(List<TaxObligation> obligations) {
        this.obligations = obligations != null ? obligations : new ArrayList<>();
    }
}
