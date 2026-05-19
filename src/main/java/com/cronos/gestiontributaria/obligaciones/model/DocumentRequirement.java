package com.cronos.gestiontributaria.obligaciones.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class DocumentRequirement {

    private String id;
    private String name; // e.g., "Subir extracto bancario"
    private RequirementStatus status;
    private String documentId; // the id of the Document once uploaded
    private LocalDateTime requestedAt;
    private LocalDateTime fulfilledAt;

    public enum RequirementStatus {
        PENDING,
        FULFILLED
    }

    public DocumentRequirement() {
        this.id = UUID.randomUUID().toString();
        this.status = RequirementStatus.PENDING;
        this.requestedAt = LocalDateTime.now();
    }

    public DocumentRequirement(String name) {
        this();
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RequirementStatus getStatus() {
        return status;
    }

    public void setStatus(RequirementStatus status) {
        this.status = status;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getFulfilledAt() {
        return fulfilledAt;
    }

    public void setFulfilledAt(LocalDateTime fulfilledAt) {
        this.fulfilledAt = fulfilledAt;
    }
}
