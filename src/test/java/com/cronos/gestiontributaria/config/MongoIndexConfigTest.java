package com.cronos.gestiontributaria.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;

/**
 * Tests sobre {@link MongoIndexConfig} usando mocks de {@link MongoTemplate}
 * y {@link MongoCollection}.
 *
 * No se usa {@code @DataMongoTest} porque el classpath de test no incluye
 * un MongoDB embebido (flapdoodle / testcontainers no están como dependencia).
 * Igualmente se valida el contrato esperado: que los 3 índices se piden con
 * los nombres y las opciones correctas (incluyendo {@code sparse=true} para
 * el índice de obligationId).
 */
@ExtendWith(MockitoExtension.class)
class MongoIndexConfigTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @SuppressWarnings("unchecked")
    @Mock
    private MongoCollection<org.bson.Document> documentosCollection;

    @InjectMocks
    private MongoIndexConfig mongoIndexConfig;

    private ArgumentCaptor<IndexOptions> optionsCaptor;
    private ArgumentCaptor<Bson> keysCaptor;

    @BeforeEach
    void setUp() {
        when(mongoTemplate.getCollection("documentos")).thenReturn(documentosCollection);
        keysCaptor = ArgumentCaptor.forClass(Bson.class);
        optionsCaptor = ArgumentCaptor.forClass(IndexOptions.class);
        mongoIndexConfig.initIndexes();
        verify(documentosCollection, times(3))
                .createIndex(keysCaptor.capture(), optionsCaptor.capture());
    }

    @Test
    void indiceTaxPayerIdExiste() {
        List<String> nombres = optionsCaptor.getAllValues().stream()
                .map(IndexOptions::getName)
                .toList();
        assertTrue(nombres.contains("idx_documentos_taxPayerId"),
                "Falta el índice idx_documentos_taxPayerId; encontrados: " + nombres);
    }

    @Test
    void indiceObligationIdEsSparse() {
        IndexOptions opts = optionsCaptor.getAllValues().stream()
                .filter(o -> "idx_documentos_obligationId".equals(o.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Falta el índice idx_documentos_obligationId"));
        assertEquals(Boolean.TRUE, opts.isSparse(),
                "El índice idx_documentos_obligationId debe ser sparse");
    }

    @Test
    void indiceTaxPayerFechaExiste() {
        List<String> nombres = optionsCaptor.getAllValues().stream()
                .map(IndexOptions::getName)
                .toList();
        assertTrue(nombres.contains("idx_documentos_taxPayer_fecha"),
                "Falta el índice idx_documentos_taxPayer_fecha; encontrados: " + nombres);
    }

    @Test
    void colaboraConLaColeccionDocumentos() {
        // Verifica que se opera específicamente sobre la colección "documentos".
        verify(mongoTemplate).getCollection("documentos");
        verify(documentosCollection, times(3))
                .createIndex(any(Bson.class), any(IndexOptions.class));
    }
}
