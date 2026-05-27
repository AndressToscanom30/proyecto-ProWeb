package com.cronos.gestiontributaria.solicitudes.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Tipos de solicitud permitidos en el sistema.
 *
 * <p>Define las categorías válidas que un usuario puede seleccionar
 * al radicar una solicitud. Los valores permitidos son exactamente:
 * SOPORTE, ACCESO, INFORMACIÓN.</p>
 */
public enum TipoSolicitud {
    SOPORTE("SOPORTE"),
    ACCESO("ACCESO"),
    INFORMACION("INFORMACIÓN");

    private final String valor;

    TipoSolicitud(String valor) {
        this.valor = valor;
    }

    /**
     * Serializa el enum con su nombre correcto en español (con tilde).
     */
    @JsonValue
    public String getValor() {
        return valor;
    }

    /**
     * Deserializa aceptando tanto "INFORMACIÓN" como "INFORMACION".
     *
     * @param texto valor recibido en el JSON
     * @return el tipo de solicitud correspondiente
     */
    @JsonCreator
    public static TipoSolicitud fromString(String texto) {
        if (texto == null) {
            return null;
        }
        for (TipoSolicitud tipo : values()) {
            if (tipo.valor.equalsIgnoreCase(texto) || tipo.name().equalsIgnoreCase(texto)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Tipo de solicitud no válido: " + texto
                + ". Valores permitidos: SOPORTE, ACCESO, INFORMACIÓN");
    }
}
