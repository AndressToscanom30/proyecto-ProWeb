package com.cronos.gestiontributaria.clientes.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.cronos.gestiontributaria.common.TaxpayerType;
import com.cronos.gestiontributaria.notification.model.Notification;
import com.cronos.gestiontributaria.obligaciones.model.Document;
import com.cronos.gestiontributaria.obligaciones.model.TaxObligation;

public class TaxPayer {
    private String businessName;
    private TaxpayerType type;
    private String email;
    private String phone;
    private String address;
    private boolean active;
    private LocalDate registrationDate;
    private List<TaxObligation> obligations;
    private List<BankAccount> bankAccounts;
    private List<Notification> notifications;

    public TaxPayer(){
        
    }

    public TaxPayer(String businessName, TaxpayerType type, String email, String phone,
            String address, boolean active, LocalDate registrationDate,
            List<TaxObligation> obligations, List<BankAccount> bankAccounts,
            List<Notification> notifications) {
        this.businessName = businessName;
        this.type = type;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.active = active;
        this.registrationDate = registrationDate;
        this.obligations = obligations != null ? obligations : new ArrayList<>();
        this.bankAccounts = bankAccounts != null ? bankAccounts : new ArrayList<>();
        this.notifications = notifications != null ? notifications : new ArrayList<>();
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public List<TaxObligation> getObligations() {
        return obligations;
    }

    public List<Document> getDocuments() {
        List<Document> docs = new ArrayList<>();
        if (obligations != null) {
            for (TaxObligation obligation : obligations) {
                if (obligation != null && obligation.getDocuments() != null) {
                    docs.addAll(obligation.getDocuments());
                }
            }
        }
        return docs;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public TaxpayerType getType() {
        return type;
    }

    public void setType(TaxpayerType type) {
        this.type = type;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDate getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDate registrationDate) {
        this.registrationDate = registrationDate;
    }

    public List<BankAccount> getBankAccounts() {
        return bankAccounts;
    }

    public void setBankAccounts(List<BankAccount> bankAccounts) {
        this.bankAccounts = bankAccounts != null ? bankAccounts : new ArrayList<>();
    }

    public List<Notification> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<Notification> notifications) {
        this.notifications = notifications != null ? notifications : new ArrayList<>();
    }

    public void setObligations(List<TaxObligation> obligations) {
        this.obligations = obligations != null ? obligations : new ArrayList<>();
    }
}
