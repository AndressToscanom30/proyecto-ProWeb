package com.cronos.gestiontributaria.obligaciones.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;

import com.cronos.gestiontributaria.calendarios.model.Alert;
import com.cronos.gestiontributaria.common.TaxObligationStatus;
import com.cronos.gestiontributaria.common.TaxObligationType;
import com.cronos.gestiontributaria.empleados.model.Task;

@org.springframework.data.mongodb.core.mapping.Document(collection = "obligaciones")
/**
 * Documentación de la entidad TaxObligation.
 */
public class TaxObligation {
    @Id
    private String id;
    private TaxObligationType type;
    private String taxPayerId;              // referencia al contribuyente
    private String fiscalPeriod;
    private int taxYear;
    private LocalDate dueDate;
    private boolean dueDateOverridden;      // true si la dueDate fue ingresada manualmente
    private String dueDateOverrideReason;   // obligatorio si dueDateOverridden=true (auditoría)
    private ObligationAssignment counterResponsible;
    private ObligationAssignment auxiliaryResponsible;
    private TaxObligationStatus status;
    private String notes;
    /**
     * IDs de los documentos asociados a esta obligación.
     * Los documentos viven en su propia colección "documentos"
     * (ver {@link Document}); aquí solo guardamos referencias por id.
     */
    private List<String> documentIds = new ArrayList<>();
    private List<Alert> alerts;
    private List<TaxIndicator> indicators;
    private List<Task> tasks;
    private List<DocumentRequirement> documentRequirements = new ArrayList<>();

    public TaxObligation(){

    }

    public TaxObligation(TaxObligationType type, String taxPayerId, String fiscalPeriod,
                        int taxYear, LocalDate dueDate, boolean dueDateOverridden,
                        String dueDateOverrideReason, TaxObligationStatus status, String notes,
                        List<String> documentIds, List<Alert> alerts,
                        List<TaxIndicator> indicators, List<Task> tasks) {
        this.type = type;
        this.taxPayerId = taxPayerId;
        this.fiscalPeriod = fiscalPeriod;
        this.taxYear = taxYear;
        this.dueDate = dueDate;
        this.dueDateOverridden = dueDateOverridden;
        this.dueDateOverrideReason = dueDateOverrideReason;
        this.status = status;
        this.notes = notes;
        this.documentIds = documentIds != null ? documentIds : new ArrayList<>();
        this.alerts = alerts != null ? alerts : new ArrayList<>();
        this.indicators = indicators != null ? indicators : new ArrayList<>();
        this.tasks = tasks != null ? tasks : new ArrayList<>();
        this.counterResponsible = null;
        this.auxiliaryResponsible = null;
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

    // ─── Getters y Setters ────────────────────────────────────────────────

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TaxObligationType getType() {
        return type;
    }

    public void setType(TaxObligationType type) {
        this.type = type;
    }

    public String getTaxPayerId() {
        return taxPayerId;
    }

    public void setTaxPayerId(String taxPayerId) {
        this.taxPayerId = taxPayerId;
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

    public boolean isDueDateOverridden() {
        return dueDateOverridden;
    }

    public void setDueDateOverridden(boolean dueDateOverridden) {
        this.dueDateOverridden = dueDateOverridden;
    }

    public String getDueDateOverrideReason() {
        return dueDateOverrideReason;
    }

    public void setDueDateOverrideReason(String dueDateOverrideReason) {
        this.dueDateOverrideReason = dueDateOverrideReason;
    }

    public ObligationAssignment getCounterResponsible() {
        return counterResponsible;
    }

    public void setCounterResponsible(ObligationAssignment counterResponsible) {
        this.counterResponsible = counterResponsible;
    }

    public ObligationAssignment getAuxiliaryResponsible() {
        return auxiliaryResponsible;
    }

    public void setAuxiliaryResponsible(ObligationAssignment auxiliaryResponsible) {
        this.auxiliaryResponsible = auxiliaryResponsible;
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

    public List<String> getDocumentIds() {
        return documentIds;
    }

    public void setDocumentIds(List<String> documentIds) {
        this.documentIds = documentIds != null ? documentIds : new ArrayList<>();
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

    public List<DocumentRequirement> getDocumentRequirements() {
        return documentRequirements;
    }

    public void setDocumentRequirements(List<DocumentRequirement> documentRequirements) {
        this.documentRequirements = documentRequirements != null ? documentRequirements : new ArrayList<>();
    }
}
