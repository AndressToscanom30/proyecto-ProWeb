package com.cronos.gestiontributaria.clientes.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.cronos.gestiontributaria.clientes.model.TaxPayer;
import com.cronos.gestiontributaria.clientes.repository.TaxPayerRepository;
import com.cronos.gestiontributaria.common.TaxpayerType;

/**

 * Documentación de la entidad TaxPayerService.

 */

@Service
public class TaxPayerService {

    private final TaxPayerRepository repository;

    /**
     * Inyección opcional para no romper tests / contextos donde MongoTemplate
     * no esté configurado.
     */
    @Autowired(required = false)
    private MongoTemplate mongoTemplate;

    public TaxPayerService(TaxPayerRepository repository) {
        this.repository = repository;
    }

    public List<TaxPayer> findAll() {
        return repository.findAll();
    }

    /**
     * Búsqueda paginada con filtros opcionales.
     *
     * <p>Todos los parámetros son opcionales: {@code null} = sin filtro.
     * Si {@code searchTerm} viene en blanco se interpreta como sin filtro.</p>
     *
     * @param searchTerm texto a buscar en {@code businessName} o {@code identificacion}
     * @param type       filtrar por {@link TaxpayerType} (null = todos)
     * @param active     filtrar por estado (null = todos)
     * @param pageable   paginación y ordenamiento
     * @return página de contribuyentes
     */
    public Page<TaxPayer> findByFilters(String searchTerm,
                                        TaxpayerType type,
                                        Boolean active,
                                        Pageable pageable) {
        if (mongoTemplate != null) {
            return findByFiltersWithTemplate(searchTerm, type, active, pageable);
        }
        // Fallback degradado: si no hay MongoTemplate (no debería pasar en
        // producción), devuelve la página plana del repositorio sin filtros.
        Page<TaxPayer> all = repository.findAll(pageable);
        return all;
    }

    private Page<TaxPayer> findByFiltersWithTemplate(String searchTerm,
                                                     TaxpayerType type,
                                                     Boolean active,
                                                     Pageable pageable) {
        Query query = new Query();
        List<Criteria> criteria = new ArrayList<>();

        if (searchTerm != null && !searchTerm.isBlank()) {
            String term = searchTerm.trim();
            criteria.add(new Criteria().orOperator(
                    Criteria.where("businessName").regex(term, "i"),
                    Criteria.where("identificacion").regex(term, "i")
            ));
        }
        if (type != null) {
            criteria.add(Criteria.where("type").is(type));
        }
        if (active != null) {
            criteria.add(Criteria.where("active").is(active));
        }
        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(
                    criteria.toArray(new Criteria[0])));
        }

        long total = mongoTemplate.count(query, TaxPayer.class);
        query.with(pageable);
        List<TaxPayer> results = mongoTemplate.find(query, TaxPayer.class);
        return new PageImpl<>(results, pageable, total);
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

        // Campos editables desde el formulario. Se protegen contra nulos
        // críticos para evitar corromper la BD si se bypassa la validación del form.
        if (incoming.getBusinessName() != null && !incoming.getBusinessName().isBlank()) {
            existing.setBusinessName(incoming.getBusinessName());
        }
        if (incoming.getIdentificacion() != null && !incoming.getIdentificacion().isBlank()) {
            existing.setIdentificacion(incoming.getIdentificacion());
        }
        if (incoming.getType() != null) {
            existing.setType(incoming.getType());
        }
        if (incoming.getEmail() != null && !incoming.getEmail().isBlank()) {
            existing.setEmail(incoming.getEmail());
        }
        
        // Campos que sí pueden ser actualizados a nulo o vacío (opcionales)
        existing.setPhone(incoming.getPhone());
        existing.setAddress(incoming.getAddress());
        
        // Booleanos (no pueden ser null, son primitivos boolean)
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

    /**
     * Invierte el estado activo del contribuyente y lo persiste.
     *
     * @param id ID del contribuyente
     * @return contribuyente actualizado con el nuevo estado
     * @throws NoSuchElementException si no existe un contribuyente con ese id
     */
    public TaxPayer toggleActive(String id) {
        TaxPayer existing = findById(id);
        if (existing.isActive()) {
            existing.deactivate();
        } else {
            existing.activate();
        }
        return repository.save(existing);
    }

    public long count() {
        return repository.count();
    }
}
