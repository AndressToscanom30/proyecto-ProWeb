package com.cronos.gestiontributaria.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;

import jakarta.annotation.PostConstruct;

/**
 * Define los índices de MongoDB necesarios para las consultas más
 * frecuentes del módulo de documentos.
 *
 * Los índices se crean al arrancar la aplicación si aún no existen
 * (createIndex es idempotente en MongoDB cuando se especifica un nombre).
 */
@Configuration
public class MongoIndexConfig {

    @Autowired
    private MongoTemplate mongoTemplate;

    @PostConstruct
    public void initIndexes() {
        MongoCollection<org.bson.Document> col =
                mongoTemplate.getCollection("documentos");

        // Búsqueda de todos los documentos de un contribuyente.
        col.createIndex(
                new org.bson.Document("taxPayerId", 1),
                new IndexOptions().name("idx_documentos_taxPayerId")
        );

        // Búsqueda por obligación asociada (sparse: puede ser null).
        col.createIndex(
                new org.bson.Document("obligationId", 1),
                new IndexOptions().name("idx_documentos_obligationId")
                        .sparse(true)
        );

        // Listado por contribuyente ordenado por fecha de subida descendente.
        col.createIndex(
                new org.bson.Document("taxPayerId", 1)
                        .append("uploadedAt", -1),
                new IndexOptions().name("idx_documentos_taxPayer_fecha")
        );
    }
}
