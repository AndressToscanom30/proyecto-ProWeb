package com.cronos.gestiontributaria.notification.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**

 * Documentación de la entidad Notification.

 */

public class Notification {
    private String id;
    private String title;
    private String message;
    private String type;
    private boolean read;
    private LocalDateTime createdAt;
    private String sourceId;
    private String targetUrl;

    public Notification(){
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
    }

    public Notification(String message, String type, boolean read) {
        this();
        this.title = "Notificación";
        this.message = message;
        this.type = type;
        this.read = read;
    }

    public Notification(String title, String message, String type, boolean read,
                        LocalDateTime createdAt, String sourceId, String targetUrl) {
        this();
        this.title = title;
        this.message = message;
        this.type = type;
        this.read = read;
        this.createdAt = createdAt;
        this.sourceId = sourceId;
        this.targetUrl = targetUrl;
    }

    public void markAsRead() {
        this.read = true;
    }

    public void markAsUnread() {
        this.read = false;
    }

    public void delete() {
        this.title = "";
        this.message = "";
        this.type = "";
        this.read = true;
        this.sourceId = "";
        this.targetUrl = "";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }
}
