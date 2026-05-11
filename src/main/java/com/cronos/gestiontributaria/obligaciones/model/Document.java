package com.cronos.gestiontributaria.obligaciones.model;

public class Document {
    private String name;
    private String description;
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
        if (newObligation != null && newObligation.getDocuments() != null
                && !newObligation.getDocuments().contains(this)) {
            newObligation.getDocuments().add(this);
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
