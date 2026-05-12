package com.cronos.gestiontributaria.common;

/**

 * Documentación de la entidad EmployeeRole.

 */

public enum EmployeeRole {
    GERENTE("Gerente General"),
    CONTADOR("Contador Principal"),
    AUXILIAR_CONTADOR("Auxiliar Contable"),
    CONTRIBUYENTE("Contribuyente (Cliente)");

    private final String description;

    EmployeeRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
