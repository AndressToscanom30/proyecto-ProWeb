package com.cronos.gestiontributaria.obligaciones.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import com.cronos.gestiontributaria.auth.model.User;
import com.cronos.gestiontributaria.auth.repository.UserRepository;
import com.cronos.gestiontributaria.clientes.model.TaxPayer;
import com.cronos.gestiontributaria.clientes.repository.TaxPayerRepository;
import com.cronos.gestiontributaria.common.TaxObligationStatus;
import com.cronos.gestiontributaria.common.TaxObligationType;
import com.cronos.gestiontributaria.common.EmployeeRole;
import com.cronos.gestiontributaria.obligaciones.dto.CreateTaxObligationDTO;
import com.cronos.gestiontributaria.obligaciones.dto.ObligationAssignmentDTO;
import com.cronos.gestiontributaria.obligaciones.dto.TaxObligationResponseDTO;
import com.cronos.gestiontributaria.obligaciones.model.ObligationAssignment;
import com.cronos.gestiontributaria.obligaciones.model.TaxObligation;
import com.cronos.gestiontributaria.obligaciones.repository.TaxObligationRepository;
import com.cronos.gestiontributaria.notification.service.NotificationMailService;
import com.cronos.gestiontributaria.notification.service.NotificationEmailDetail;

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
    private final UserRepository userRepository;
    private final NotificationMailService notificationMailService;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    // Patrones de validación para fiscalPeriod
    private static final Pattern MONTHLY = Pattern.compile("^\\d{4}-\\d{2}$");
    private static final Pattern BIMONTHLY = Pattern.compile("^\\d{4}-B[1-6]$");
    private static final Pattern QUARTERLY = Pattern.compile("^\\d{4}-Q[1-3]$");
    private static final Pattern ANNUAL = Pattern.compile("^\\d{4}$");

    public TaxObligationService(TaxObligationRepository repository,
                                 TaxObligationDateResolver dateResolver,
                                 TaxPayerRepository taxPayerRepository,
                                 UserRepository userRepository,
                                 NotificationMailService notificationMailService) {
        this.repository = repository;
        this.dateResolver = dateResolver;
        this.taxPayerRepository = taxPayerRepository;
        this.userRepository = userRepository;
        this.notificationMailService = notificationMailService;
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
        obligation.setCounterResponsible(buildAssignment(
            resolveResponsibleUser(dto.counterResponsibleId(), EmployeeRole.CONTADOR),
            resolveCurrentActor()));
        obligation.setAuxiliaryResponsible(null);

        TaxObligation saved = repository.save(obligation);
        TaxObligationResponseDTO response = toResponseDTO(saved, taxpayer);
        notifyInternalResponsibles(response, taxpayer, "Nueva obligación asignada", "La obligación fue registrada y asignada a tu equipo.");
        return response;
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
     * Lista todas las obligaciones registradas en el sistema.
     */
    public List<TaxObligationResponseDTO> findAll() {
        return repository.findAll().stream()
                .map(o -> {
                    TaxPayer taxpayer = taxPayerRepository.findById(o.getTaxPayerId()).orElse(null);
                    return toResponseDTO(o, taxpayer);
                })
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

        boolean dueDateChanged = dto.dueDateOverride() != null
            && (existing.getDueDate() == null || !dto.dueDateOverride().equals(existing.getDueDate()));

        if (dto.dueDateOverride() != null && dueDateChanged) {
            if (dto.dueDateOverrideReason() == null || dto.dueDateOverrideReason().isBlank()) {
                throw new IllegalArgumentException(
                "Se requiere dueDateOverrideReason cuando se modifica la fecha de vencimiento");
            }
            existing.setDueDate(dto.dueDateOverride());
            existing.setDueDateOverridden(true);
            existing.setDueDateOverrideReason(dto.dueDateOverrideReason());
        } else if (dto.dueDateOverride() == null) {
            existing.setDueDate(existing.getDueDate());
            existing.setDueDateOverridden(existing.isDueDateOverridden());
            existing.setDueDateOverrideReason(existing.getDueDateOverrideReason());
        }

        TaxObligation saved = repository.save(existing);
        TaxObligationResponseDTO response = toResponseDTO(saved, taxpayer);
        if (dueDateChanged) {
            notifyInternalResponsibles(response, taxpayer, "Cambio de fecha de vencimiento", "La fecha de vencimiento de una obligación fue modificada.");
        }
        return response;
    }

        /**
         * Reasigna el contador responsable de una obligación.
         */
        public TaxObligationResponseDTO assignCounter(String id, String counterUserId) {
        TaxObligation obligation = repository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Obligación no encontrada con ID: " + id));
        TaxPayer taxpayer = taxPayerRepository.findById(obligation.getTaxPayerId()).orElse(null);
        User actor = resolveCurrentActor();
        User counter = resolveResponsibleUser(counterUserId, EmployeeRole.CONTADOR);
        ObligationAssignment previousCounter = obligation.getCounterResponsible();

        obligation.setCounterResponsible(buildAssignment(counter, actor));
        TaxObligation saved = repository.save(obligation);
        TaxObligationResponseDTO response = toResponseDTO(saved, taxpayer);

        notifyUser(counter.getEmail(), "Nuevo contador responsable asignado",
            "Nueva obligación asignada",
            "Se te ha asignado o reasignado una obligación tributaria.",
            buildObligationNotificationDetails(taxpayer, response, actor,
                List.of(new NotificationEmailDetail("Observación", "Eres el contador principal de esta obligación."))),
            "Ver obligación",
            buildObligationUrl(response != null ? response.id() : null));
        if (previousCounter != null && previousCounter.getUserEmail() != null
            && !previousCounter.getUserEmail().isBlank()
            && !previousCounter.getUserEmail().equalsIgnoreCase(counter.getEmail())) {
            notificationMailService.sendStructuredEmail(
                previousCounter.getUserEmail(),
                "Reasignación de contador responsable",
                "Reasignación de contador",
                "La responsabilidad principal de una obligación fue reasignada.",
                buildObligationNotificationDetails(taxpayer, response, actor,
                    List.of(new NotificationEmailDetail("Observación", "Tu asignación anterior fue reemplazada."))),
                "Ver obligación",
                buildObligationUrl(response != null ? response.id() : null));
        }
        notifyAuxiliaryIfPresent(response, taxpayer, actor, "El contador responsable fue actualizado.");
        return response;
        }

        /**
         * Asigna o cambia el auxiliar responsable de una obligación.
         */
        public TaxObligationResponseDTO assignAuxiliary(String id, String auxiliaryUserId) {
        TaxObligation obligation = repository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Obligación no encontrada con ID: " + id));
        TaxPayer taxpayer = taxPayerRepository.findById(obligation.getTaxPayerId()).orElse(null);
        User actor = resolveCurrentActor();
        User auxiliary = auxiliaryUserId == null || auxiliaryUserId.isBlank()
            ? null
            : resolveResponsibleUser(auxiliaryUserId, EmployeeRole.AUXILIAR_CONTADOR);
        ObligationAssignment previousAuxiliary = obligation.getAuxiliaryResponsible();

        obligation.setAuxiliaryResponsible(auxiliary != null ? buildAssignment(auxiliary, actor) : null);
        TaxObligation saved = repository.save(obligation);
        TaxObligationResponseDTO response = toResponseDTO(saved, taxpayer);

        if (auxiliary != null) {
            notifyUser(auxiliary.getEmail(), "Auxiliar responsable asignado",
                "Asignación de auxiliar",
                "Se te asignó una obligación como auxiliar responsable.",
                buildObligationNotificationDetails(taxpayer, response, actor,
                    List.of(new NotificationEmailDetail("Observación", "Eres el auxiliar responsable actual."))),
                "Ver obligación",
                buildObligationUrl(response != null ? response.id() : null));
        }
        if (previousAuxiliary != null && previousAuxiliary.getUserEmail() != null
            && !previousAuxiliary.getUserEmail().isBlank()
            && (auxiliary == null || !previousAuxiliary.getUserEmail().equalsIgnoreCase(auxiliary.getEmail()))) {
            notificationMailService.sendStructuredEmail(
                previousAuxiliary.getUserEmail(),
                "Cambio de auxiliar responsable",
                "Cambio de auxiliar",
                "Tu asignación como auxiliar responsable fue reemplazada.",
                buildObligationNotificationDetails(taxpayer, response, actor,
                    List.of(new NotificationEmailDetail("Observación", "Tu asignación anterior fue reemplazada."))),
                "Ver obligación",
                buildObligationUrl(response != null ? response.id() : null));
        }

        notifyCounterIfPresent(response, taxpayer, actor, "El auxiliar responsable fue actualizado.");
        return response;
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
        TaxObligationResponseDTO response = toResponseDTO(saved, taxpayer);
        notifyInternalResponsibles(response, taxpayer, "Cambio de estado de obligación", "El estado de una obligación fue actualizado.");
        return response;
    }

    /**
     * Añade un requerimiento de documento a la obligación.
     */
    public TaxObligationResponseDTO addRequirement(String obligationId, String requirementName) {
        TaxObligation obligation = repository.findById(obligationId)
                .orElseThrow(() -> new NoSuchElementException("Obligación no encontrada con ID: " + obligationId));
        
        com.cronos.gestiontributaria.obligaciones.model.DocumentRequirement req = 
                new com.cronos.gestiontributaria.obligaciones.model.DocumentRequirement(requirementName);
        
        obligation.getDocumentRequirements().add(req);
        TaxObligation saved = repository.save(obligation);
        
        TaxPayer taxpayer = taxPayerRepository.findById(saved.getTaxPayerId()).orElse(null);
        TaxObligationResponseDTO response = toResponseDTO(saved, taxpayer);
        if (taxpayer != null && taxpayer.getEmail() != null && !taxpayer.getEmail().isBlank()) {
            notificationMailService.sendStructuredEmail(
                taxpayer.getEmail(),
                "Solicitud de documentos — Cronos",
                "Solicitud de documentos",
                "Se te ha solicitado un nuevo documento para cumplir esta obligación.",
                buildClientNotificationDetails(taxpayer, response,
                    requirementName,
                    null),
                "Cargar documentos",
                buildPortalUrl(response != null ? response.id() : null));
        }
        return response;
    }

    /**
     * Marca un requerimiento como cumplido, asociando el documento subido.
     */
    public TaxObligationResponseDTO fulfillRequirement(String obligationId, String requirementId, String documentId) {
        TaxObligation obligation = repository.findById(obligationId)
                .orElseThrow(() -> new NoSuchElementException("Obligación no encontrada con ID: " + obligationId));
        
        boolean found = false;
        for (com.cronos.gestiontributaria.obligaciones.model.DocumentRequirement req : obligation.getDocumentRequirements()) {
            if (req.getId().equals(requirementId)) {
                req.setStatus(com.cronos.gestiontributaria.obligaciones.model.DocumentRequirement.RequirementStatus.FULFILLED);
                req.setDocumentId(documentId);
                req.setFulfilledAt(LocalDate.now().atStartOfDay());
                found = true;
                break;
            }
        }
        
        if (!found) {
            throw new NoSuchElementException("Requerimiento no encontrado con ID: " + requirementId);
        }
        
        TaxObligation saved = repository.save(obligation);
        TaxPayer taxpayer = taxPayerRepository.findById(saved.getTaxPayerId()).orElse(null);
        TaxObligationResponseDTO response = toResponseDTO(saved, taxpayer);
        if (taxpayer != null && taxpayer.getEmail() != null && !taxpayer.getEmail().isBlank()) {
            notificationMailService.sendStructuredEmail(
                taxpayer.getEmail(),
                "Confirmación de documentos recibidos — Cronos",
                "Confirmación de recepción",
                "Hemos recibido correctamente el documento cargado para esta obligación.",
                buildClientNotificationDetails(taxpayer, response, null, documentId),
                "Ver documentos",
                buildPortalUrl(response != null ? response.id() : null));
        }
        return response;
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
                o.getNotes(),
                toAssignmentDTO(o.getCounterResponsible()),
                toAssignmentDTO(o.getAuxiliaryResponsible()),
                o.getDocumentRequirements()
        );
    }

    private ObligationAssignmentDTO toAssignmentDTO(ObligationAssignment assignment) {
        if (assignment == null) {
            return null;
        }
        return new ObligationAssignmentDTO(
                assignment.getUserId(),
                assignment.getUserName(),
                assignment.getUserEmail(),
                assignment.getAssignedById(),
                assignment.getAssignedByName(),
                assignment.getAssignedByEmail(),
                assignment.getAssignedAt());
    }

    private ObligationAssignment buildAssignment(User responsible, User actor) {
        if (responsible == null) {
            return null;
        }
        return new ObligationAssignment(
                responsible.getId(),
                responsible.getName(),
                responsible.getEmail(),
                actor != null ? actor.getId() : null,
                actor != null ? actor.getName() : "Sistema",
                actor != null ? actor.getEmail() : null,
                LocalDateTime.now());
    }

    private User resolveCurrentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return null;
        }
        return userRepository.findByEmail(authentication.getName()).orElse(null);
    }

    private User resolveResponsibleUser(String userId, EmployeeRole expectedRole) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Se requiere un responsable válido");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado con ID: " + userId));
        String expectedRoleName = "ROLE_" + expectedRole.name();
        String actualRoleName = user.getRole() != null && user.getRole().getName() != null
                ? user.getRole().getName().trim().toUpperCase()
                : "";
        if (!expectedRoleName.equals(actualRoleName)) {
            throw new IllegalArgumentException("El usuario seleccionado no tiene el rol requerido: " + expectedRole.name());
        }
        return user;
    }

    private void notifyInternalResponsibles(TaxObligationResponseDTO response, TaxPayer taxpayer,
            String eventTitle, String intro) {
        User actor = resolveCurrentActor();
        notifyCounterIfPresent(response, taxpayer, actor, intro);
        notifyAuxiliaryIfPresent(response, taxpayer, actor, intro);
    }

    private void notifyCounterIfPresent(TaxObligationResponseDTO response, TaxPayer taxpayer, User actor, String intro) {
        if (response == null || response.counterResponsible() == null || response.counterResponsible().userEmail() == null
                || response.counterResponsible().userEmail().isBlank()) {
            return;
        }
        notificationMailService.sendStructuredEmail(
                response.counterResponsible().userEmail(),
                buildSubject("Contador responsable", response),
            "Contador responsable",
            intro,
            buildObligationNotificationDetails(taxpayer, response, actor, null),
            "Ver obligación",
            buildObligationUrl(response != null ? response.id() : null));
    }

    private void notifyAuxiliaryIfPresent(TaxObligationResponseDTO response, TaxPayer taxpayer, User actor, String intro) {
        if (response == null || response.auxiliaryResponsible() == null || response.auxiliaryResponsible().userEmail() == null
                || response.auxiliaryResponsible().userEmail().isBlank()) {
            return;
        }
        notificationMailService.sendStructuredEmail(
                response.auxiliaryResponsible().userEmail(),
                buildSubject("Auxiliar responsable", response),
            "Auxiliar responsable",
            intro,
            buildObligationNotificationDetails(taxpayer, response, actor, null),
            "Ver obligación",
            buildObligationUrl(response != null ? response.id() : null));
    }

    private String buildSubject(String eventTitle, TaxObligationResponseDTO response) {
        String client = response != null && response.taxPayerName() != null ? response.taxPayerName() : "Cliente";
        return eventTitle + " · " + client;
    }

    private String buildAssignmentEmailBody(String eventTitle, TaxPayer taxpayer, TaxObligationResponseDTO response,
            User actor, String intro) {
        StringBuilder builder = new StringBuilder();
        builder.append(intro != null ? intro : "Actualización del módulo de obligaciones.").append("\n\n");
        builder.append("Evento: ").append(eventTitle).append("\n");
        if (taxpayer != null) {
            builder.append("Cliente: ").append(taxpayer.getBusinessName()).append("\n");
            builder.append("Identificación: ").append(taxpayer.getIdentificacion()).append("\n");
        }
        if (response != null) {
            builder.append("Obligación: ").append(response.type() != null ? response.type().getDescription() : "—").append("\n");
            builder.append("Periodo fiscal: ").append(response.fiscalPeriod() != null ? response.fiscalPeriod() : "—").append("\n");
            builder.append("Año gravable: ").append(response.taxYear()).append("\n");
            builder.append("Vencimiento: ").append(response.dueDate() != null ? response.dueDate() : "—").append("\n");
            builder.append("Estado: ").append(response.status() != null ? response.status().name().replace('_', ' ') : "—").append("\n");
        }
        if (actor != null) {
            builder.append("Asignado por: ").append(actor.getName() != null ? actor.getName() : actor.getEmail()).append("\n");
        }
        builder.append("Acción: Ver obligación\n");
        builder.append("Enlace: ").append(buildAbsoluteUrl(buildObligationUrl(response != null ? response.id() : null)));
        return builder.toString();
    }

    private String buildObligationUrl(String obligationId) {
        return obligationId != null && !obligationId.isBlank() ? "/obligaciones/" + obligationId : "/obligaciones";
    }

    private String buildPortalUrl(String obligationId) {
        return obligationId != null && !obligationId.isBlank() ? "/portal/obligaciones/" + obligationId : "/portal/documentos";
    }

    private String buildAbsoluteUrl(String path) {
        if (path == null || path.isBlank()) {
            return appBaseUrl;
        }
        String normalizedBase = appBaseUrl != null ? appBaseUrl.trim() : "http://localhost:8080";
        if (normalizedBase.endsWith("/")) {
            normalizedBase = normalizedBase.substring(0, normalizedBase.length() - 1);
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        return normalizedBase + (path.startsWith("/") ? path : "/" + path);
    }

    private List<NotificationEmailDetail> buildObligationNotificationDetails(TaxPayer taxpayer,
            TaxObligationResponseDTO response, User actor, List<NotificationEmailDetail> extraDetails) {
        List<NotificationEmailDetail> details = new ArrayList<>();
        if (taxpayer != null) {
            details.add(new NotificationEmailDetail("Cliente", taxpayer.getBusinessName()));
            details.add(new NotificationEmailDetail("Identificación", taxpayer.getIdentificacion()));
        }
        if (response != null) {
            details.add(new NotificationEmailDetail("Obligación",
                    response.type() != null && response.type().getDescription() != null
                            ? response.type().getDescription()
                            : (response.type() != null ? response.type().name() : "—")));
            details.add(new NotificationEmailDetail("Periodo fiscal", response.fiscalPeriod() != null ? response.fiscalPeriod() : "—"));
            details.add(new NotificationEmailDetail("Año gravable", String.valueOf(response.taxYear())));
            details.add(new NotificationEmailDetail("Vencimiento", response.dueDate() != null ? response.dueDate().toString() : "—"));
            details.add(new NotificationEmailDetail("Estado",
                    response.status() != null ? response.status().name().replace('_', ' ') : "—"));
        }
        if (actor != null) {
            details.add(new NotificationEmailDetail("Asignado por",
                    actor.getName() != null && !actor.getName().isBlank() ? actor.getName() : actor.getEmail()));
        }
        if (extraDetails != null && !extraDetails.isEmpty()) {
            details.addAll(extraDetails);
        }
        return details;
    }

    private List<NotificationEmailDetail> buildClientNotificationDetails(TaxPayer taxpayer,
            TaxObligationResponseDTO response, String requirementName, String documentId) {
        List<NotificationEmailDetail> details = new ArrayList<>();
        if (taxpayer != null) {
            details.add(new NotificationEmailDetail("Cliente", taxpayer.getBusinessName()));
        }
        if (response != null) {
            details.add(new NotificationEmailDetail("Obligación",
                    response.type() != null && response.type().getDescription() != null
                            ? response.type().getDescription()
                            : (response.type() != null ? response.type().name() : "—")));
            details.add(new NotificationEmailDetail("Periodo fiscal", response.fiscalPeriod() != null ? response.fiscalPeriod() : "—"));
            details.add(new NotificationEmailDetail("Vencimiento", response.dueDate() != null ? response.dueDate().toString() : "—"));
        }
        if (requirementName != null && !requirementName.isBlank()) {
            details.add(new NotificationEmailDetail("Documento solicitado", requirementName));
        }
        if (documentId != null && !documentId.isBlank()) {
            details.add(new NotificationEmailDetail("Documento recibido", documentId));
        }
        return details;
    }

    private void notifyUser(String recipientEmail, String subject, String headline, String intro,
            List<NotificationEmailDetail> details, String actionLabel, String actionPath) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            return;
        }
        notificationMailService.sendStructuredEmail(recipientEmail, subject, headline, intro, details, actionLabel, actionPath);
    }
}
