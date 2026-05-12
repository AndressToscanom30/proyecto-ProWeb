package com.cronos.gestiontributaria.obligaciones.dto;

import java.time.LocalDate;

import com.cronos.gestiontributaria.common.TaxObligationStatus;
import com.cronos.gestiontributaria.common.TaxObligationType;
import com.cronos.gestiontributaria.common.TaxpayerType;

/**
 * DTO de respuesta para obligaciones tributarias.
 *
 * <p>Incluye los datos de la obligación junto con información contextual
 * del contribuyente asociado (nombre, identificación, tipo).</p>
 *
 * @param id                    ID de la obligación en MongoDB
 * @param taxPayerId            ID del contribuyente
 * @param taxPayerName          nombre o razón social del contribuyente
 * @param taxPayerIdentificacion NIT o CC del contribuyente
 * @param taxPayerType          tipo de contribuyente (NATURAL_PERSON o LEGAL_ENTITY)
 * @param type                  tipo de obligación tributaria
 * @param fiscalPeriod          período fiscal
 * @param taxYear               año gravable
 * @param dueDate               fecha de vencimiento
 * @param dueDateOverridden     indica si la fecha fue ingresada manualmente
 * @param dueDateOverrideReason razón del override (si aplica)
 * @param status                estado actual de la obligación
 * @param notes                 notas adicionales
 */
public record TaxObligationResponseDTO(
    String id,
    String taxPayerId,
    String taxPayerName,
    String taxPayerIdentificacion,
    TaxpayerType taxPayerType,
    TaxObligationType type,
    String fiscalPeriod,
    int taxYear,
    LocalDate dueDate,
    boolean dueDateOverridden,
    String dueDateOverrideReason,
    TaxObligationStatus status,
    String notes
) {}
