package com.cronos.gestiontributaria.obligaciones.service;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.cronos.gestiontributaria.clientes.model.TaxPayer;
import com.cronos.gestiontributaria.clientes.repository.TaxPayerRepository;
import com.cronos.gestiontributaria.common.TaxObligationStatus;
import com.cronos.gestiontributaria.common.TaxObligationType;
import com.cronos.gestiontributaria.obligaciones.dto.CreateTaxObligationDTO;
import com.cronos.gestiontributaria.obligaciones.dto.TaxObligationResponseDTO;
import com.cronos.gestiontributaria.obligaciones.model.TaxObligation;
import com.cronos.gestiontributaria.obligaciones.repository.TaxObligationRepository;

import java.util.NoSuchElementException;

/**
 * Servicio principal para la gestión de obligaciones tributarias.
 *
 * <p>Coordina la creación, consulta, actualización y eliminación de obligaciones,
 * aplicando las reglas de negocio del calendario tributario colombiano.</p>
 */
@Service
public class TaxObligationService {

    private final TaxObligationRepository repository;
    private final TaxObligationDateResolver dateResolver;
    private final TaxPayerRepository taxPayerRepository;

    // Patrones de validación para fiscalPeriod
    private static final Pattern MONTHLY = Pattern.compile("^\\d{4}-\\d{2}$");
    private static final Pattern BIMONTHLY = Pattern.compile("^\\d{4}-B[1-6]$");
    private static final Pattern QUARTERLY = Pattern.compile("^\\d{4}-Q[1-3]$");
    private static final Pattern ANNUAL = Pattern.compile("^\\d{4}$");

    public TaxObligationService(TaxObligationRepository repository,
                                 TaxObligationDateResolver dateResolver,
                                 TaxPayerRepository taxPayerRepository) {
        this.repository = repository;
        this.dateResolver = dateResolver;
        this.taxPayerRepository = taxPayerRepository;
    }

    /**
     * Crea una nueva obligación tributaria con cálculo automático de fecha.
     */
    public TaxObligationResponseDTO create(CreateTaxObligationDTO dto) {
        TaxPayer taxpayer = taxPayerRepository.findById(dto.taxPayerId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Contribuyente no encontrado con ID: " + dto.taxPayerId()));

        // Validar que el contribuyente esté activo
        if (!taxpayer.isActive()) {
            throw new IllegalArgumentException(
                    "No se puede crear una obligación para un contribuyente inactivo");
        }

        // Validar formato de fiscalPeriod según tipo de obligación
        validateFiscalPeriod(dto.type(), dto.fiscalPeriod());

        // Validar unicidad compuesta: type + fiscalPeriod por TaxPayer
        if (repository.existsByTaxPayerIdAndTypeAndFiscalPeriod(
                dto.taxPayerId(), dto.type(), dto.fiscalPeriod())) {
            throw new IllegalArgumentException(
                    "Ya existe una obligación " + dto.type() + " para el período " +
                    dto.fiscalPeriod() + " del contribuyente " + dto.taxPayerId());
        }

        LocalDate dueDate;
        boolean overridden = false;

        if (dto.dueDateOverride() != null) {
            if (dto.dueDateOverrideReason() == null || dto.dueDateOverrideReason().isBlank()) {
                throw new IllegalArgumentException(
                        "Se requiere dueDateOverrideReason cuando se sobreescribe la fecha");
            }
            dueDate = dto.dueDateOverride();
            overridden = true;
        } else {
            dueDate = dateResolver.resolve(
                    dto.type(), dto.fiscalPeriod(),
                    taxpayer.getIdentificacion(), taxpayer.getType(),
                    taxpayer.isGranContribuyente());

            if (dueDate == null) {
                throw new IllegalArgumentException(
                        "INDUSTRY_COMMERCE requiere ingresar la fecha manualmente (varía por municipio)");
            }
        }

        TaxObligation obligation = new TaxObligation();
        obligation.setType(dto.type());
        obligation.setTaxPayerId(dto.taxPayerId());
        obligation.setFiscalPeriod(dto.fiscalPeriod());
        obligation.setTaxYear(dto.taxYear() != null ? dto.taxYear() : 2026);
        obligation.setDueDate(dueDate);
        obligation.setDueDateOverridden(overridden);
        obligation.setDueDateOverrideReason(overridden ? dto.dueDateOverrideReason() : null);
        obligation.setStatus(TaxObligationStatus.PENDING);
        obligation.setNotes(dto.notes());

        TaxObligation saved = repository.save(obligation);
        return toResponseDTO(saved, taxpayer);
    }

    /**
     * Busca una obligación por su ID.
     */
    public TaxObligationResponseDTO findById(String id) {
        TaxObligation obligation = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Obligación no encontrada con ID: " + id));
        TaxPayer taxpayer = taxPayerRepository.findById(obligation.getTaxPayerId())
                .orElse(null);
        return toResponseDTO(obligation, taxpayer);
    }

    /**
     * Lista las obligaciones de un contribuyente.
     */
    public List<TaxObligationResponseDTO> findByTaxPayerId(String taxPayerId) {
        TaxPayer taxpayer = taxPayerRepository.findById(taxPayerId).orElse(null);
        return repository.findByTaxPayerId(taxPayerId).stream()
                .map(o -> toResponseDTO(o, taxpayer))
                .toList();
    }

    /**
     * Actualiza una obligación existente.
     */
    public TaxObligationResponseDTO update(String id, CreateTaxObligationDTO dto) {
        TaxObligation existing = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Obligación no encontrada con ID: " + id));

        TaxPayer taxpayer = taxPayerRepository.findById(dto.taxPayerId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Contribuyente no encontrado con ID: " + dto.taxPayerId()));

        validateFiscalPeriod(dto.type(), dto.fiscalPeriod());

        existing.setType(dto.type());
        existing.setTaxPayerId(dto.taxPayerId());
        existing.setFiscalPeriod(dto.fiscalPeriod());
        existing.setTaxYear(dto.taxYear() != null ? dto.taxYear() : existing.getTaxYear());
        existing.setNotes(dto.notes());

        if (dto.dueDateOverride() != null) {
            if (dto.dueDateOverrideReason() == null || dto.dueDateOverrideReason().isBlank()) {
                throw new IllegalArgumentException(
                        "Se requiere dueDateOverrideReason cuando se sobreescribe la fecha");
            }
            existing.setDueDate(dto.dueDateOverride());
            existing.setDueDateOverridden(true);
            existing.setDueDateOverrideReason(dto.dueDateOverrideReason());
        } else {
            LocalDate dueDate = dateResolver.resolve(
                    dto.type(), dto.fiscalPeriod(),
                    taxpayer.getIdentificacion(), taxpayer.getType(),
                    taxpayer.isGranContribuyente());
            if (dueDate == null && dto.type() == TaxObligationType.INDUSTRY_COMMERCE) {
                throw new IllegalArgumentException(
                        "INDUSTRY_COMMERCE requiere fecha manual");
            }
            existing.setDueDate(dueDate);
            existing.setDueDateOverridden(false);
            existing.setDueDateOverrideReason(null);
        }

        return toResponseDTO(repository.save(existing), taxpayer);
    }

    /**
     * Elimina una obligación por su ID.
     */
    public void delete(String id) {
        if (!repository.existsById(id)) {
            throw new NoSuchElementException("Obligación no encontrada con ID: " + id);
        }
        repository.deleteById(id);
    }

    /**
     * Cambia el estado de una obligación.
     */
    public TaxObligationResponseDTO changeStatus(String id, TaxObligationStatus newStatus) {
        TaxObligation obligation = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Obligación no encontrada con ID: " + id));
        obligation.changeStatus(newStatus);
        TaxObligation saved = repository.save(obligation);
        TaxPayer taxpayer = taxPayerRepository.findById(saved.getTaxPayerId()).orElse(null);
        return toResponseDTO(saved, taxpayer);
    }

    // ─── Validaciones ─────────────────────────────────────────────────────

    private void validateFiscalPeriod(TaxObligationType type, String fiscalPeriod) {
        if (fiscalPeriod == null || fiscalPeriod.isBlank()) {
            throw new IllegalArgumentException("fiscalPeriod es obligatorio");
        }
        boolean valid = switch (type) {
            case WITHHOLDING -> MONTHLY.matcher(fiscalPeriod).matches();
            case VAT -> BIMONTHLY.matcher(fiscalPeriod).matches()
                     || QUARTERLY.matcher(fiscalPeriod).matches();
            case INCOME_TAX, PATRIMONY -> ANNUAL.matcher(fiscalPeriod).matches();
            case INDUSTRY_COMMERCE -> true; // sin restricción de formato
        };
        if (!valid) {
            throw new IllegalArgumentException(
                    "Formato de fiscalPeriod inválido '" + fiscalPeriod +
                    "' para el tipo " + type);
        }
    }

    // ─── Mapeo ────────────────────────────────────────────────────────────

    private TaxObligationResponseDTO toResponseDTO(TaxObligation o, TaxPayer tp) {
        return new TaxObligationResponseDTO(
                o.getId(),
                o.getTaxPayerId(),
                tp != null ? tp.getBusinessName() : null,
                tp != null ? tp.getIdentificacion() : null,
                tp != null ? tp.getType() : null,
                o.getType(),
                o.getFiscalPeriod(),
                o.getTaxYear(),
                o.getDueDate(),
                o.isDueDateOverridden(),
                o.getDueDateOverrideReason(),
                o.getStatus(),
                o.getNotes()
        );
    }
}
