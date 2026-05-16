package com.cronos.gestiontributaria.clientes.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.cronos.gestiontributaria.common.TaxpayerType;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Tests unitarios de Bean Validation sobre la entidad {@link TaxPayer}.
 * No levantan el contexto de Spring: usan el Validator de Jakarta directamente.
 */
class TaxPayerValidationTest {

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

    private TaxPayer nuevoTaxPayerValido() {
        TaxPayer tp = new TaxPayer();
        tp.setBusinessName("Empresa Test S.A.S");
        tp.setIdentificacion("900123456-1");
        tp.setType(TaxpayerType.LEGAL_ENTITY);
        tp.setEmail("contacto@empresa.com");
        return tp;
    }

    @Test
    void taxPayerValido_sinViolaciones() {
        TaxPayer tp = nuevoTaxPayerValido();
        Set<ConstraintViolation<TaxPayer>> violations = validator.validate(tp);
        assertTrue(violations.isEmpty(),
                "No deberían existir violaciones, pero hay: " + violations);
    }

    @Test
    void businessNameEnBlanco_unaViolacion() {
        TaxPayer tp = nuevoTaxPayerValido();
        tp.setBusinessName("");
        Set<ConstraintViolation<TaxPayer>> violations = validator.validate(tp);
        assertEquals(1, violations.size(), "Debe haber exactamente 1 violación");
        assertEquals("businessName", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void emailInvalido_unaViolacion() {
        TaxPayer tp = nuevoTaxPayerValido();
        tp.setEmail("no-es-un-email");
        Set<ConstraintViolation<TaxPayer>> violations = validator.validate(tp);
        assertEquals(1, violations.size(), "Debe haber exactamente 1 violación");
        assertEquals("email", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void typeNull_unaViolacion() {
        TaxPayer tp = nuevoTaxPayerValido();
        tp.setType(null);
        Set<ConstraintViolation<TaxPayer>> violations = validator.validate(tp);
        assertEquals(1, violations.size(), "Debe haber exactamente 1 violación");
        assertEquals("type", violations.iterator().next().getPropertyPath().toString());
    }
}
