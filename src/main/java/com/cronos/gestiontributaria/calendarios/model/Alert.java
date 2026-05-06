package com.cronos.gestiontributaria.calendarios.model;

import java.time.LocalDateTime;

public class Alert {
    private int daysInAdvance;
    private boolean sent;
    private LocalDateTime scheduledFor;
    private LocalDateTime sentAt;

    public Alert(int daysInAdvance, boolean sent, LocalDateTime scheduledFor, LocalDateTime sentAt) {
        this.daysInAdvance = daysInAdvance;
        this.sent = sent;
        this.scheduledFor = scheduledFor;
        this.sentAt = sentAt;
    }

    public void send() {
        this.sent = true;
        this.sentAt = LocalDateTime.now();
    }

    public void cancel() {
        this.sent = false;
        this.sentAt = null;
    }

    public boolean isDuplicate() {
        return sent && sentAt != null;
    }

    public int getDaysInAdvance() {
        return daysInAdvance;
    }

    public void setDaysInAdvance(int daysInAdvance) {
        this.daysInAdvance = daysInAdvance;
    }

    public boolean isSent() {
        return sent;
    }

    public void setSent(boolean sent) {
        this.sent = sent;
    }

    public LocalDateTime getScheduledFor() {
        return scheduledFor;
    }

    public void setScheduledFor(LocalDateTime scheduledFor) {
        this.scheduledFor = scheduledFor;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
}
