package com.cronos.gestiontributaria.clientes.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.cronos.gestiontributaria.clientes.model.TaxPayer;

/**
 * Repositorio MongoDB para la gestión de contribuyentes.
 */
public interface TaxPayerRepository extends MongoRepository<TaxPayer, String> {

    /**
     * Busca un contribuyente por su identificación (NIT o CC).
     *
     * @param identificacion NIT o cédula del contribuyente
     * @return contribuyente encontrado, si existe
     */
    Optional<TaxPayer> findByIdentificacion(String identificacion);

    /**
     * Indica si ya existe un contribuyente con la identificación indicada.
     *
     * @param identificacion NIT o cédula a validar
     * @return {@code true} si la identificación ya está registrada
     */
    boolean existsByIdentificacion(String identificacion);
}
