package com.cronos.gestiontributaria.clientes.model;

import com.cronos.gestiontributaria.common.AccountType;

public class BankAccount {
    private String bank;
    private String accountNumber;
    private AccountType type;
    private boolean active;

    public BankAccount(){
        
    }

    public BankAccount(String bank, String accountNumber, AccountType type, boolean active) {
        this.bank = bank;
        this.accountNumber = accountNumber;
        this.type = type;
        this.active = active;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public String getBank() {
        return bank;
    }

    public void setBank(String bank) {
        this.bank = bank;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public AccountType getType() {
        return type;
    }

    public void setType(AccountType type) {
        this.type = type;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
