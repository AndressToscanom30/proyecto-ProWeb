package com.cronos.gestiontributaria.common;

/**

 * Documentación de la entidad TaxObligationType.

 */

public enum TaxObligationType {
    INCOME_TAX("Impuesto sobre la Renta"),
    VAT("Impuesto al Valor Agregado (IVA)"),
    WITHHOLDING("Retención en la Fuente"),
    INDUSTRY_COMMERCE("Industria y Comercio (ICA)"),
    PATRIMONY("Impuesto al Patrimonio");

    private final String description;

    TaxObligationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
