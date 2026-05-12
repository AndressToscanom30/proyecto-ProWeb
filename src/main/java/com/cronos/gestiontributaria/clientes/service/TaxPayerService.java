package com.cronos.gestiontributaria.clientes.service;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import com.cronos.gestiontributaria.clientes.model.TaxPayer;
import com.cronos.gestiontributaria.clientes.repository.TaxPayerRepository;

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
        taxPayer.setRegistrationDate(LocalDate.now());
        taxPayer.setActive(true);
        return repository.save(taxPayer);
    }

    public TaxPayer update(String id, TaxPayer updated) {
        TaxPayer existing = findById(id);
        existing.setBusinessName(updated.getBusinessName());
        existing.setIdentificacion(updated.getIdentificacion());
        existing.setType(updated.getType());
        existing.setGranContribuyente(updated.isGranContribuyente());
        existing.setEmail(updated.getEmail());
        existing.setPhone(updated.getPhone());
        existing.setAddress(updated.getAddress());
        existing.setActive(updated.isActive());
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
