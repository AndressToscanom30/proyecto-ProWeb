package com.cronos.gestiontributaria.clientes.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.cronos.gestiontributaria.common.TaxpayerType;
import com.cronos.gestiontributaria.notification.model.Notification;
import com.cronos.gestiontributaria.obligaciones.model.TaxObligation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**

 * Documentación de la entidad TaxPayer.

 */

@Document(collection = "contribuyentes")
public class TaxPayer {
    @Id
    private String id;

    @NotBlank(message = "La razón social es obligatoria")
    @Size(max = 200, message = "Máximo 200 caracteres")
    private String businessName;

    @NotBlank(message = "La identificación es obligatoria")
    @Size(max = 20, message = "Máximo 20 caracteres")
    private String identificacion;       // NIT (persona jurídica) o CC (persona natural)

    @NotNull(message = "El tipo de contribuyente es obligatorio")
    private TaxpayerType type;

    private boolean granContribuyente;   // true si es clasificado como gran contribuyente por la DIAN

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato válido")
    private String email;

    @Size(max = 20, message = "Máximo 20 caracteres")
    private String phone;

    @Size(max = 300, message = "Máximo 300 caracteres")
    private String address;

    private boolean active;
    private LocalDate registrationDate;
    private List<TaxObligation> obligations;
    private List<BankAccount> bankAccounts;
    private List<Notification> notifications;

    public TaxPayer(){
        
    }

    public TaxPayer(String businessName, String identificacion, TaxpayerType type,
            boolean granContribuyente, String email, String phone,
            String address, boolean active, LocalDate registrationDate,
            List<TaxObligation> obligations, List<BankAccount> bankAccounts,
            List<Notification> notifications) {
        this.businessName = businessName;
        this.identificacion = identificacion;
        this.type = type;
        this.granContribuyente = granContribuyente;
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdentificacion() {
        return identificacion;
    }

    public void setIdentificacion(String identificacion) {
        this.identificacion = identificacion;
    }

    public boolean isGranContribuyente() {
        return granContribuyente;
    }

    public void setGranContribuyente(boolean granContribuyente) {
        this.granContribuyente = granContribuyente;
    }

    public List<TaxObligation> getObligations() {
        return obligations;
    }

    public List<com.cronos.gestiontributaria.obligaciones.model.Document> getDocuments() {
        List<com.cronos.gestiontributaria.obligaciones.model.Document> docs = new ArrayList<>();
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
