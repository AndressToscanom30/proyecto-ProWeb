package com.cronos.gestiontributaria.clientes.service;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import com.cronos.gestiontributaria.clientes.model.TaxPayer;
import com.cronos.gestiontributaria.clientes.repository.TaxPayerRepository;

/**

 * Documentación de la entidad TaxPayerService.

 */

@Service
public class TaxPayerService {

    private final TaxPayerRepository repository;

    public TaxPayerService(TaxPayerRepository repository) {
        this.repository = repository;
    }

    public List<TaxPayer> findAll() {
        return repository.findAll();
    }

    public TaxPayer findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Contribuyente no encontrado con ID: " + id));
    }

    public TaxPayer create(TaxPayer taxPayer) {
        if (taxPayer.getIdentificacion() != null && repository.existsByIdentificacion(taxPayer.getIdentificacion())) {
            throw new IllegalArgumentException("Ya existe un contribuyente con la identificación " + taxPayer.getIdentificacion());
        }
        taxPayer.setId(null);
        taxPayer.setRegistrationDate(LocalDate.now());
        taxPayer.setActive(true);
        return repository.save(taxPayer);
    }

    public TaxPayer update(String id, TaxPayer incoming) {
        TaxPayer existing = findById(id);  // lanza excepción si no existe

        // Campos editables desde el formulario
        existing.setBusinessName(incoming.getBusinessName());
        existing.setIdentificacion(incoming.getIdentificacion());
        existing.setType(incoming.getType());
        existing.setEmail(incoming.getEmail());
        existing.setPhone(incoming.getPhone());
        existing.setAddress(incoming.getAddress());
        existing.setGranContribuyente(incoming.isGranContribuyente());
        existing.setActive(incoming.isActive());

        // Colecciones embebidas: NUNCA se actualizan desde el form.
        // Se preservan siempre. Cada módulo las gestiona por su cuenta.
        // existing.setObligations(...)    ← NO
        // existing.setBankAccounts(...)   ← NO
        // existing.setNotifications(...)  ← NO

        return repository.save(existing);
    }

    public void delete(String id) {
        if (!repository.existsById(id)) {
            throw new NoSuchElementException("Contribuyente no encontrado con ID: " + id);
        }
        repository.deleteById(id);
    }

    public long count() {
        return repository.count();
    }
}
