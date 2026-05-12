package com.cronos.gestiontributaria.obligaciones.dto;

import java.time.LocalDate;

import com.cronos.gestiontributaria.common.TaxObligationType;

/**
 * DTO para la creación de una nueva obligación tributaria.
 *
 * <p>Si {@code dueDateOverride} es {@code null}, la fecha se calcula automáticamente
 * a partir de las tablas del calendario tributario colombiano (Decreto 2229/2023).
 * Si se proporciona, {@code dueDateOverrideReason} es obligatorio para auditoría.</p>
 *
 * @param taxPayerId           ID del contribuyente en MongoDB
 * @param type                 tipo de obligación (INCOME_TAX, VAT, WITHHOLDING, etc.)
 * @param fiscalPeriod         período fiscal: "2026-01" (mensual), "2026-B1" (bimestral),
 *                             "2026-Q1" (cuatrimestral), "2026" (anual)
 * @param taxYear              año gravable
 * @param notes                notas adicionales (opcional)
 * @param dueDateOverride      fecha manual de vencimiento (null = calcular automáticamente)
 * @param dueDateOverrideReason razón del override (obligatorio si dueDateOverride != null)
 */
public record CreateTaxObligationDTO(
    String taxPayerId,
    TaxObligationType type,
    String fiscalPeriod,
    Integer taxYear,
    String notes,
    LocalDate dueDateOverride,
    String dueDateOverrideReason
) {}
