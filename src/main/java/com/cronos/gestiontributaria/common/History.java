package com.cronos.gestiontributaria.common;

public class History {
    private String module;
    private String action;
    private String affectedEntity;
    private String previousValue;
    private String newValue;
    private String sourceIp;
    private String userAgent;

    public History(String module, String action, String affectedEntity, String previousValue,
                   String newValue, String sourceIp, String userAgent) {
        this.module = module;
        this.action = action;
        this.affectedEntity = affectedEntity;
        this.previousValue = previousValue;
        this.newValue = newValue;
        this.sourceIp = sourceIp;
        this.userAgent = userAgent;
    }

    public void record() {
        if (module == null) {
            module = "UNKNOWN";
        }
        if (action == null) {
            action = "UNKNOWN";
        }
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getAffectedEntity() {
        return affectedEntity;
    }

    public void setAffectedEntity(String affectedEntity) {
        this.affectedEntity = affectedEntity;
    }

    public String getPreviousValue() {
        return previousValue;
    }

    public void setPreviousValue(String previousValue) {
        this.previousValue = previousValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public void setSourceIp(String sourceIp) {
        this.sourceIp = sourceIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}
