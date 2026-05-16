package com.cronos.gestiontributaria.obligaciones.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Tests unitarios de Bean Validation sobre la entidad {@link Document}.
 * No levantan el contexto de Spring: usan el Validator de Jakarta directamente.
 */
class DocumentValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        if (factory != null) {
            factory.close();
        }
    }

    private Document nuevoDocumentoValido() {
        Document doc = new Document();
        doc.setFileName("contrato.pdf");
        doc.setStoredFileName("uuid-contrato.pdf");
        doc.setFileType("application/pdf");
        doc.setFileSize(102400L);
        doc.setTaxPayerId("tp-001");
        // obligationId queda null (opcional)
        return doc;
    }

    @Test
    void documentoValidoCompleto_sinViolaciones() {
        Document doc = nuevoDocumentoValido();
        Set<ConstraintViolation<Document>> violations = validator.validate(doc);
        assertTrue(violations.isEmpty(),
                "No deberían existir violaciones, pero hay: " + violations);
    }

    @Test
    void fileNameEnBlanco_unaViolacion() {
        Document doc = nuevoDocumentoValido();
        doc.setFileName("");
        Set<ConstraintViolation<Document>> violations = validator.validate(doc);
        assertEquals(1, violations.size(), "Debe haber exactamente 1 violación");
        assertEquals("fileName", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void taxPayerIdNulo_unaViolacion() {
        Document doc = nuevoDocumentoValido();
        doc.setTaxPayerId(null);
        Set<ConstraintViolation<Document>> violations = validator.validate(doc);
        assertEquals(1, violations.size(), "Debe haber exactamente 1 violación");
        assertEquals("taxPayerId", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void descriptionDemasiadoLarga_unaViolacion() {
        Document doc = nuevoDocumentoValido();
        doc.setDescription("a".repeat(501));
        Set<ConstraintViolation<Document>> violations = validator.validate(doc);
        assertEquals(1, violations.size(), "Debe haber exactamente 1 violación");
        assertEquals("description", violations.iterator().next().getPropertyPath().toString());
    }
}
