package com.cronos.gestiontributaria.clientes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cronos.gestiontributaria.clientes.model.BankAccount;
import com.cronos.gestiontributaria.clientes.model.TaxPayer;
import com.cronos.gestiontributaria.clientes.repository.TaxPayerRepository;
import com.cronos.gestiontributaria.common.AccountType;
import com.cronos.gestiontributaria.notification.model.Notification;
import com.cronos.gestiontributaria.obligaciones.model.TaxObligation;

/**
 * Tests unitarios de {@link TaxPayerService} sin levantar contexto de Spring.
 * Foco: el método {@code update} debe preservar las colecciones embebidas
 * (obligations, bankAccounts, notifications) que NO viajan en el formulario.
 */
@ExtendWith(MockitoExtension.class)
class TaxPayerServiceTest {

    @Mock
    private TaxPayerRepository taxPayerRepository;

    @InjectMocks
    private TaxPayerService taxPayerService;

    private TaxPayer existingBase(String id) {
        TaxPayer existing = new TaxPayer();
        existing.setId(id);
        existing.setBusinessName("Viejo Nombre");
        existing.setIdentificacion("900100200-1");
        return existing;
    }

    private TaxPayer incomingBase() {
        TaxPayer incoming = new TaxPayer();
        incoming.setBusinessName("Nuevo Nombre");
        incoming.setIdentificacion("900100200-1");
        return incoming;
    }

    private void mockRepoFind(String id, TaxPayer existing) {
        when(taxPayerRepository.findById(id)).thenReturn(Optional.of(existing));
        when(taxPayerRepository.save(any(TaxPayer.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void update_preservaObligations_cuandoIncomingEsNull() {
        TaxPayer existing = existingBase("tp-001");
        existing.setObligations(new ArrayList<>(Arrays.asList(
                new TaxObligation(), new TaxObligation())));

        TaxPayer incoming = incomingBase();
        incoming.setObligations(null);

        mockRepoFind("tp-001", existing);

        TaxPayer result = taxPayerService.update("tp-001", incoming);

        assertNotNull(result.getObligations());
        assertEquals(2, result.getObligations().size());
    }

    @Test
    void update_preservaBankAccounts_cuandoIncomingEsVacio() {
        TaxPayer existing = existingBase("tp-002");
        existing.setBankAccounts(new ArrayList<>(List.of(
                new BankAccount("Bancolombia", "1234567890", AccountType.SAVINGS, true))));

        TaxPayer incoming = incomingBase();
        incoming.setBankAccounts(new ArrayList<>());

        mockRepoFind("tp-002", existing);

        TaxPayer result = taxPayerService.update("tp-002", incoming);

        assertNotNull(result.getBankAccounts());
        assertEquals(1, result.getBankAccounts().size());
        assertEquals("Bancolombia", result.getBankAccounts().get(0).getBank());
    }

    @Test
    void update_preservaNotifications_cuandoIncomingEsNull() {
        TaxPayer existing = existingBase("tp-003");
        existing.setNotifications(new ArrayList<>(Arrays.asList(
                new Notification(), new Notification(), new Notification())));

        TaxPayer incoming = incomingBase();
        incoming.setNotifications(null);

        mockRepoFind("tp-003", existing);

        TaxPayer result = taxPayerService.update("tp-003", incoming);

        assertNotNull(result.getNotifications());
        assertEquals(3, result.getNotifications().size());
    }

    @Test
    void update_modificaCamposEditables() {
        TaxPayer existing = existingBase("tp-004");
        existing.setBusinessName("Viejo Nombre");

        TaxPayer incoming = incomingBase();
        incoming.setBusinessName("Nuevo Nombre");

        mockRepoFind("tp-004", existing);

        TaxPayer result = taxPayerService.update("tp-004", incoming);

        assertEquals("Nuevo Nombre", result.getBusinessName());
    }

    @Test
    void update_lanzaExcepcionSiIdNoExiste() {
        when(taxPayerRepository.findById("tp-missing")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(NoSuchElementException.class,
                () -> taxPayerService.update("tp-missing", incomingBase()));
        assertTrue(ex.getMessage().contains("tp-missing"),
                "El mensaje debería mencionar el id: " + ex.getMessage());
    }
}
