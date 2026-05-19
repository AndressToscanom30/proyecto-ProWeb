package com.cronos.gestiontributaria.clientes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.cronos.gestiontributaria.clientes.model.TaxPayer;
import com.cronos.gestiontributaria.clientes.repository.TaxPayerRepository;
import com.cronos.gestiontributaria.common.TaxpayerType;
import com.cronos.gestiontributaria.obligaciones.model.TaxObligation;

@SpringBootTest
@ActiveProfiles("test")
public class TaxPayerServiceIntegrationTest {

    @Autowired
    private TaxPayerService taxPayerService;

    @Autowired
    private TaxPayerRepository taxPayerRepository;

    @AfterEach
    void tearDown() {
        taxPayerRepository.deleteAll();
    }

    @Test
    void crearDuplicadoLanzaExcepcion() {
        TaxPayer tp1 = new TaxPayer();
        tp1.setBusinessName("Empresa 1");
        tp1.setIdentificacion("123456789");
        taxPayerService.create(tp1);

        TaxPayer tp2 = new TaxPayer();
        tp2.setBusinessName("Empresa 2");
        tp2.setIdentificacion("123456789"); // Mismo NIT

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            taxPayerService.create(tp2);
        });
        assertTrue(ex.getMessage().contains("Ya existe un contribuyente"));
    }

    @Test
    void updatePreservaColecciones() {
        // Crear
        TaxPayer tp = new TaxPayer();
        tp.setBusinessName("Original");
        tp.setIdentificacion("987654321");
        
        TaxObligation ob = new TaxObligation();
        ob.setFiscalPeriod("2023-01");
        ArrayList<TaxObligation> obligaciones = new ArrayList<>();
        obligaciones.add(ob);
        tp.setObligations(obligaciones);
        
        TaxPayer guardado = taxPayerRepository.save(tp);

        // Update simulando que viene del form (sin obligaciones)
        TaxPayer formUpdates = new TaxPayer();
        formUpdates.setBusinessName("Editado");
        formUpdates.setIdentificacion("987654321");

        TaxPayer actualizado = taxPayerService.update(guardado.getId(), formUpdates);

        assertEquals("Editado", actualizado.getBusinessName());
        assertFalse(actualizado.getObligations().isEmpty());
        assertEquals("2023-01", actualizado.getObligations().get(0).getFiscalPeriod());
    }

    @Test
    void toggleActivoCambiaEstado() {
        TaxPayer tp = new TaxPayer();
        tp.setBusinessName("Test Estado");
        tp.setIdentificacion("111111111");
        TaxPayer guardado = taxPayerService.create(tp);

        assertTrue(guardado.isActive());

        TaxPayer inactivo = taxPayerService.toggleActive(guardado.getId());
        assertFalse(inactivo.isActive());

        TaxPayer reactivado = taxPayerService.toggleActive(guardado.getId());
        assertTrue(reactivado.isActive());
    }

    @Test
    void findByFiltersConPaginacion() {
        TaxPayer tp1 = new TaxPayer();
        tp1.setBusinessName("Global Tech");
        tp1.setIdentificacion("NIT1");
        tp1.setType(TaxpayerType.LEGAL_ENTITY);
        taxPayerService.create(tp1);

        TaxPayer tp2 = new TaxPayer();
        tp2.setBusinessName("Global Corp");
        tp2.setIdentificacion("NIT2");
        tp2.setType(TaxpayerType.LEGAL_ENTITY);
        taxPayerService.create(tp2);

        TaxPayer tp3 = new TaxPayer();
        tp3.setBusinessName("Juan Perez");
        tp3.setIdentificacion("NIT3");
        tp3.setType(TaxpayerType.NATURAL_PERSON);
        taxPayerService.create(tp3);

        Page<TaxPayer> pagina = taxPayerService.findByFilters(
                "Global", TaxpayerType.LEGAL_ENTITY, true, PageRequest.of(0, 10));

        assertEquals(2, pagina.getTotalElements());
        assertTrue(pagina.getContent().stream().allMatch(t -> t.getBusinessName().startsWith("Global")));
    }
}
