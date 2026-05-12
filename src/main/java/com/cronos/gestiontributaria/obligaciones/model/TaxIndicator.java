package com.cronos.gestiontributaria.obligaciones.model;

/**

 * Documentación de la entidad TaxIndicator.

 */

public class TaxIndicator {
    private String name;
    private String code;
    private double value;
    private String unit;
    private int effectiveYear;
    private boolean active;

    public TaxIndicator(){
        
    }

    public TaxIndicator(String name, String code, double value, String unit, int effectiveYear, boolean active) {
        this.name = name;
        this.code = code;
        this.value = value;
        this.unit = unit;
        this.effectiveYear = effectiveYear;
        this.active = active;
    }

    public double getCurrentValue() {
        return value;
    }

    public void deactivate() {
        this.active = false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public int getEffectiveYear() {
        return effectiveYear;
    }

    public void setEffectiveYear(int effectiveYear) {
        this.effectiveYear = effectiveYear;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
