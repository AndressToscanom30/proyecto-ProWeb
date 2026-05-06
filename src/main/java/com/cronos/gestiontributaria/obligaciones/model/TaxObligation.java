package com.cronos.gestiontributaria.obligaciones.model;

import com.cronos.gestiontributaria.calendarios.model.Alert;
import com.cronos.gestiontributaria.common.TaxObligationStatus;
import com.cronos.gestiontributaria.common.TaxObligationType;
import com.cronos.gestiontributaria.empleados.model.Task;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TaxObligation {
    private TaxObligationType type;
    private String fiscalPeriod;
    private int taxYear;
    private LocalDate dueDate;
    private TaxObligationStatus status;
    private String notes;
    private List<Document> documents;
    private List<Alert> alerts;
    private List<TaxIndicator> indicators;
    private List<Task> tasks;

    public TaxObligation(TaxObligationType type, String fiscalPeriod, int taxYear, LocalDate dueDate,
                         TaxObligationStatus status, String notes,
                         List<Document> documents, List<Alert> alerts,
                         List<TaxIndicator> indicators, List<Task> tasks) {
        this.type = type;
        this.fiscalPeriod = fiscalPeriod;
        this.taxYear = taxYear;
        this.dueDate = dueDate;
        this.status = status;
        this.notes = notes;
        this.documents = documents != null ? documents : new ArrayList<>();
        this.alerts = alerts != null ? alerts : new ArrayList<>();
        this.indicators = indicators != null ? indicators : new ArrayList<>();
        this.tasks = tasks != null ? tasks : new ArrayList<>();
    }

    public LocalDate calculateDueDate() {
        if (dueDate != null) {
            return dueDate;
        }
        return LocalDate.of(taxYear, 12, 31);
    }

    public void changeStatus(TaxObligationStatus newStatus) {
        if (newStatus != null) {
            this.status = newStatus;
        }
    }

    public boolean isOverdue() {
        if (dueDate == null) {
            return false;
        }
        if (TaxObligationStatus.COMPLETED.equals(status) || TaxObligationStatus.CANCELLED.equals(status)) {
            return false;
        }
        return dueDate.isBefore(LocalDate.now());
    }

    public TaxObligationType getType() {
        return type;
    }

    public void setType(TaxObligationType type) {
        this.type = type;
    }

    public String getFiscalPeriod() {
        return fiscalPeriod;
    }

    public void setFiscalPeriod(String fiscalPeriod) {
        this.fiscalPeriod = fiscalPeriod;
    }

    public int getTaxYear() {
        return taxYear;
    }

    public void setTaxYear(int taxYear) {
        this.taxYear = taxYear;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public TaxObligationStatus getStatus() {
        return status;
    }

    public void setStatus(TaxObligationStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<Document> getDocuments() {
        return documents;
    }

    public void setDocuments(List<Document> documents) {
        this.documents = documents != null ? documents : new ArrayList<>();
    }

    public List<Alert> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<Alert> alerts) {
        this.alerts = alerts != null ? alerts : new ArrayList<>();
    }

    public List<TaxIndicator> getIndicators() {
        return indicators;
    }

    public void setIndicators(List<TaxIndicator> indicators) {
        this.indicators = indicators != null ? indicators : new ArrayList<>();
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks != null ? tasks : new ArrayList<>();
    }
}
