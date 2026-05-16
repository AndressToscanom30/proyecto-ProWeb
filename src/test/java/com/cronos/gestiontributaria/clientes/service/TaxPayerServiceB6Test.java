package com.cronos.gestiontributaria.clientes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.util.ReflectionTestUtils;

import com.cronos.gestiontributaria.clientes.model.TaxPayer;
import com.cronos.gestiontributaria.clientes.repository.TaxPayerRepository;
import com.cronos.gestiontributaria.common.TaxpayerType;

/**
 * Tests para los filtros y paginación introducidos en el paso B-6.
 *
 * <p>Foco: el servicio construye la {@link Query} correcta para MongoTemplate
 * según los filtros que recibe, y devuelve una {@link Page} consistente con
 * la cuenta y los resultados del template.</p>
 */
@ExtendWith(MockitoExtension.class)
class TaxPayerServiceB6Test {

    @Mock
    private TaxPayerRepository taxPayerRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private TaxPayerService taxPayerService;

    private Pageable pageable;

    @BeforeEach
    void setUp() {
        // El campo mongoTemplate es @Autowired(required=false), así que el
        // @InjectMocks no siempre lo cablea: lo seteamos explícitamente.
        ReflectionTestUtils.setField(taxPayerService, "mongoTemplate", mongoTemplate);
        pageable = PageRequest.of(0, 20);
    }

    private void mockFind(List<TaxPayer> resultados, long total) {
        when(mongoTemplate.find(any(Query.class), eq(TaxPayer.class)))
                .thenReturn(resultados);
        when(mongoTemplate.count(any(Query.class), eq(TaxPayer.class)))
                .thenReturn(total);
    }

    @Test
    void sinFiltros_retornaTodosPaginados() {
        List<TaxPayer> esperados = List.of(new TaxPayer(), new TaxPayer(), new TaxPayer());
        mockFind(esperados, 3L);

        Page<TaxPayer> resultado =
                taxPayerService.findByFilters(null, null, null, pageable);

        assertNotNull(resultado);
        assertEquals(3, resultado.getContent().size());
        assertEquals(3L, resultado.getTotalElements());
    }

    @Test
    void filtroSearchTerm_construyeQueryConRegex() {
        mockFind(List.of(new TaxPayer()), 1L);

        taxPayerService.findByFilters("empresa", null, null, pageable);

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).count(captor.capture(), eq(TaxPayer.class));
        Document queryDoc = captor.getValue().getQueryObject();
        assertFalse(queryDoc.isEmpty(),
                "La query debe contener criterios cuando hay searchTerm: " + queryDoc);
        assertTrue(queryDoc.toJson().toLowerCase().contains("empresa"),
                "La query debe incluir el término 'empresa': " + queryDoc.toJson());
    }

    @Test
    void filtroPorType_agregaCriteriaDeTipo() {
        mockFind(List.of(new TaxPayer()), 1L);

        taxPayerService.findByFilters(null, TaxpayerType.LEGAL_ENTITY, null, pageable);

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).count(captor.capture(), eq(TaxPayer.class));
        Document queryDoc = captor.getValue().getQueryObject();
        // No usamos toJson() porque BSON no tiene codec para el enum
        // TaxpayerType; inspeccionamos el documento directamente.
        assertFalse(queryDoc.isEmpty(),
                "La query debe tener criterios cuando hay type filter");
        assertTrue(queryDoc.containsKey("type") || queryDoc.containsKey("$and"),
                "La query debe filtrar por type. Keys: " + queryDoc.keySet());
        assertTrue(queryString(queryDoc).contains("type"),
                "La query debe mencionar el campo 'type'. Keys: " + queryDoc.keySet());
    }

    /**
     * Representación de la query sin serializar valores que requieren codec
     * (como el enum TaxpayerType). Solo expone las claves.
     */
    private String queryString(Document doc) {
        StringBuilder sb = new StringBuilder("{");
        for (String key : doc.keySet()) {
            sb.append(key).append(",");
            Object val = doc.get(key);
            if (val instanceof Document nested) {
                sb.append(queryString(nested));
            } else if (val instanceof Iterable<?> it) {
                for (Object o : it) {
                    if (o instanceof Document nested) {
                        sb.append(queryString(nested));
                    }
                }
            }
        }
        sb.append("}");
        return sb.toString();
    }

    @Test
    void filtroPorActiveFalse_agregaCriteriaDeEstado() {
        mockFind(List.of(), 0L);

        taxPayerService.findByFilters(null, null, Boolean.FALSE, pageable);

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).count(captor.capture(), eq(TaxPayer.class));
        Document queryDoc = captor.getValue().getQueryObject();
        assertFalse(queryDoc.isEmpty(),
                "La query debe tener criterios cuando se filtra por active");
        assertTrue(queryDoc.toJson().contains("active"),
                "La query debe incluir el campo active: " + queryDoc.toJson());
    }

    @Test
    void searchTermEnBlanco_seTrataComoNull() {
        mockFind(List.of(), 0L);

        taxPayerService.findByFilters("   ", null, null, pageable);

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).count(captor.capture(), eq(TaxPayer.class));
        Document queryDoc = captor.getValue().getQueryObject();
        // Sin filtros válidos, la query debe quedar vacía (igual que sin parámetros).
        assertTrue(queryDoc.isEmpty(),
                "Un searchTerm en blanco no debe agregar criterios. Query: " + queryDoc.toJson());
    }
}
