package com.cronos.gestiontributaria.obligaciones.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cronos.gestiontributaria.obligaciones.model.Document;
import com.cronos.gestiontributaria.obligaciones.repository.DocumentRepository;
import com.cronos.gestiontributaria.obligaciones.repository.TaxObligationRepository;

/**
 * Servicio responsable de la subida, consulta y eliminación de documentos
 * cargados por los contribuyentes.
 *
 * <p>Los metadatos viven en la colección {@code documentos} de MongoDB
 * (ver {@link DocumentRepository}). El archivo físico se guarda en una
 * carpeta local configurable mediante {@code cronos.storage.upload-dir}.</p>
 *
 * <p>Esta implementación intencionalmente NO usa GridFS, S3 ni almacenamiento
 * externo. La ruta de almacenamiento es fácilmente reemplazable más adelante.</p>
 */
@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    @Value("${cronos.storage.upload-dir}")
    private String uploadDir;

    @Value("${cronos.storage.max-file-size-mb}")
    private int maxFileSizeMb;

    @Value("${cronos.storage.allowed-types}")
    private String[] allowedTypes;

    private final DocumentRepository documentRepository;

    private final TaxObligationRepository taxObligationRepository;

    public DocumentService(DocumentRepository documentRepository,
                           TaxObligationRepository taxObligationRepository) {
        this.documentRepository = documentRepository;
        this.taxObligationRepository = taxObligationRepository;
    }

    /**
     * Sube un archivo y guarda sus metadatos.
     *
     * @param taxPayerId   ID del contribuyente propietario
     * @param obligationId ID de la obligación asociada (puede ser {@code null})
     * @param file         archivo subido por el usuario
     * @param description  descripción opcional del contribuyente
     * @return documento persistido con sus metadatos
     * @throws IllegalArgumentException si el tipo o tamaño no son válidos
     * @throws RuntimeException si falla la escritura en disco
     */
    public Document uploadDocument(String taxPayerId,
                                   String obligationId,
                                   MultipartFile file,
                                   String description) {
        validarArchivo(file);

        String storedFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path destino = Paths.get(uploadDir).toAbsolutePath().resolve(storedFileName);

        try {
            Files.createDirectories(destino.getParent());
            file.transferTo(destino.toFile());
        } catch (IOException e) {
            throw new IllegalStateException("Error al guardar el archivo: "
                    + e.getMessage(), e);
        }

        Document doc = new Document();
        doc.setFileName(file.getOriginalFilename());
        doc.setStoredFileName(storedFileName);
        doc.setFileType(file.getContentType());
        doc.setFileSize(file.getSize());
        doc.setUploadedAt(LocalDateTime.now());
        doc.setTaxPayerId(taxPayerId);
        doc.setObligationId(obligationId);   // puede ser null
        doc.setDescription(description);

        Document saved = documentRepository.save(doc);

        // Si tiene obligación, registrar el id en documentIds.
        if (obligationId != null) {
            taxObligationRepository.findById(obligationId).ifPresent(ob -> {
                ob.getDocumentIds().add(saved.getId());
                taxObligationRepository.save(ob);
            });
        }

        return saved;
    }

    /**
     * Retorna todos los documentos de un contribuyente, ordenados por fecha
     * de subida descendente (los más recientes primero).
     */
    public List<Document> findByTaxPayer(String taxPayerId) {
        return documentRepository
                .findByTaxPayerIdOrderByUploadedAtDesc(taxPayerId);
    }

    /**
     * Retorna todos los documentos asociados a una obligación.
     */
    public List<Document> findByObligation(String obligationId) {
        return documentRepository.findByObligationId(obligationId);
    }

    /**
     * Busca un documento por su ID.
     *
     * @param documentId identificador del documento
     * @return documento persistido
     * @throws NoSuchElementException si no existe
     */
    public Document findById(String documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Documento no encontrado con ID: " + documentId));
    }

    /**
     * Elimina un documento (uso interno / GERENTE).
     * Elimina también el archivo físico si existe.
     */
    public void delete(String documentId) {
        documentRepository.findById(documentId).ifPresent(doc -> {
            Path archivo = Paths.get(uploadDir).toAbsolutePath().resolve(doc.getStoredFileName());
            try {
                Files.deleteIfExists(archivo);
            } catch (IOException e) {
                // El registro se borra igual aunque falle el borrado físico.
                log.warn("No se pudo borrar el archivo físico {}", archivo, e);
            }
            documentRepository.delete(doc);
        });
    }

    // ── privado ──────────────────────────────────────────────

    private static final List<String> ALLOWED_EXTENSIONS = List.of(
            ".pdf", ".jpg", ".jpeg", ".png", ".xlsx"
    );

    private void validarArchivo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo no puede estar vacío.");
        }

        // Validar tamaño
        long maxBytes = (long) maxFileSizeMb * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            log.warn("Archivo rechazado por tamaño: {} bytes (máx {} MB) — nombre: {}",
                    file.getSize(), maxFileSizeMb, file.getOriginalFilename());
            throw new IllegalArgumentException(
                    "El archivo supera el tamaño máximo permitido de "
                            + maxFileSizeMb + " MB.");
        }

        // Validar extensión del nombre de archivo (defensa en profundidad)
        String nombreOriginal = file.getOriginalFilename();
        if (nombreOriginal != null && !nombreOriginal.isBlank()) {
            String extensionLower = nombreOriginal.substring(
                    nombreOriginal.lastIndexOf('.')).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(extensionLower)) {
                log.warn("Archivo rechazado por extensión: {} — MIME declarado: {}",
                nombreOriginal, file.getContentType());
                throw new IllegalArgumentException(
                    "Extensión de archivo no permitida.");
            }
        }

        // Validar tipo MIME
        String tipo = file.getContentType();
        boolean tipoPermitido = Arrays.asList(allowedTypes).contains(tipo);
        if (!tipoPermitido) {
            log.warn("Archivo rechazado por tipo MIME: {} — nombre: {}",
                tipo, file.getOriginalFilename());
            throw new IllegalArgumentException(
                "Tipo de archivo no permitido: " + tipo
                    + ". Solo se aceptan PDF, JPG, PNG y XLSX.");
        }
    }
}
