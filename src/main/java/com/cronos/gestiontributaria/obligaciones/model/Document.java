package com.cronos.gestiontributaria.obligaciones.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Documentación de la entidad Document.
 *
 * Representa un archivo asociado a un contribuyente y, opcionalmente, a una
 * obligación tributaria concreta. La subida real del archivo se implementa
 * en un paso posterior; aquí solo se modelan los metadatos.
 *
 * Persistencia: colección propia "documentos" (no embebida) para permitir
 * listar todos los documentos de un contribuyente sin recorrer obligaciones.
 */
@org.springframework.data.mongodb.core.mapping.Document(collection = "documentos")
public class Document {

    // ─── Identificación ──────────────────────────────────────────────────
    @Id
    private String id;

    // ─── Metadatos del archivo subido ────────────────────────────────────
    @NotBlank(message = "El nombre del archivo es obligatorio")
    private String fileName;          // nombre original del archivo subido

    private String storedFileName;    // nombre con el que se guarda en disco (UUID + ext)

    @NotBlank(message = "El tipo de archivo es obligatorio")
    private String fileType;          // tipo MIME (ej: "application/pdf")

    @NotNull(message = "El tamaño del archivo es obligatorio")
    private Long fileSize;            // tamaño en bytes

    private LocalDateTime uploadedAt; // fecha y hora de subida

    @Size(max = 500, message = "Máximo 500 caracteres")
    private String description;       // descripción opcional ingresada por el contribuyente

    // ─── Asociaciones ────────────────────────────────────────────────────
    @NotBlank(message = "El contribuyente es obligatorio")
    private String taxPayerId;        // ID del TaxPayer propietario

    private String obligationId;      // ID de la TaxObligation asociada (OPCIONAL)

    // ─── Campos legacy preservados (no eliminar) ─────────────────────────
    private String name;
    private String storagePath;
    private String format;
    private long sizeBytes;

    public Document(){

    }

    public Document(String name, String description, String storagePath, String format, long sizeBytes) {
        this.name = name;
        this.description = description;
        this.storagePath = storagePath;
        this.format = format;
        this.sizeBytes = sizeBytes;
    }

    public void move(TaxObligation newObligation) {
        if (newObligation != null && this.id != null
                && newObligation.getDocumentIds() != null
                && !newObligation.getDocumentIds().contains(this.id)) {
            newObligation.getDocumentIds().add(this.id);
            this.obligationId = newObligation.getId();
        }
    }

    public void delete() {
        this.storagePath = "";
        this.sizeBytes = 0L;
    }

    public String getUrl() {
        if (storagePath == null || storagePath.isEmpty()) {
            return "";
        }
        if (storagePath.startsWith("http://") || storagePath.startsWith("https://")) {
            return storagePath;
        }
        return "file:///" + storagePath.replace("\\", "/");
    }

    // ─── Getters y setters de campos nuevos ──────────────────────────────

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getStoredFileName() {
        return storedFileName;
    }

    public void setStoredFileName(String storedFileName) {
        this.storedFileName = storedFileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getTaxPayerId() {
        return taxPayerId;
    }

    public void setTaxPayerId(String taxPayerId) {
        this.taxPayerId = taxPayerId;
    }

    public String getObligationId() {
        return obligationId;
    }

    public void setObligationId(String obligationId) {
        this.obligationId = obligationId;
    }

    // ─── Getters y setters legacy (no eliminar) ──────────────────────────

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }
}
