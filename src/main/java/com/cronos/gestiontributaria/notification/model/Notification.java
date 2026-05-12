package com.cronos.gestiontributaria.notification.model;

/**

 * Documentación de la entidad Notification.

 */

public class Notification {
    private String message;
    private String type;
    private boolean read;

    public Notification(){
        
    }

    public Notification(String message, String type, boolean read) {
        this.message = message;
        this.type = type;
        this.read = read;
    }

    public void markAsRead() {
        this.read = true;
    }

    public void delete() {
        this.message = "";
        this.type = "";
        this.read = true;
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
}
