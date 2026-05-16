package com.cronos.gestiontributaria.obligaciones.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.cronos.gestiontributaria.obligaciones.model.Document;

/**
 * Repositorio MongoDB para la colección "documentos".
 *
 * Las consultas más frecuentes son por taxPayerId (vista del portal del
 * contribuyente) y por obligationId (vista de detalle de una obligación).
 * Los índices correspondientes están definidos en {@code MongoIndexConfig}.
 */
public interface DocumentRepository extends MongoRepository<Document, String> {

    /**
     * Todos los documentos cargados por un contribuyente.
     */
    List<Document> findByTaxPayerId(String taxPayerId);

    /**
     * Documentos asociados a una obligación tributaria concreta.
     */
    List<Document> findByObligationId(String obligationId);

    /**
     * Listado del contribuyente ordenado por fecha de subida descendente
     * (los más recientes primero).
     */
    List<Document> findByTaxPayerIdOrderByUploadedAtDesc(String taxPayerId);

    /**
     * Indica si el contribuyente ya tiene al menos un documento cargado para
     * la obligación indicada.
     */
    boolean existsByTaxPayerIdAndObligationId(String taxPayerId,
                                              String obligationId);
}
