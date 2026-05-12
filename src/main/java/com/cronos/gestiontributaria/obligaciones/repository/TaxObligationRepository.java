package com.cronos.gestiontributaria.obligaciones.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.cronos.gestiontributaria.common.TaxObligationType;
import com.cronos.gestiontributaria.obligaciones.model.TaxObligation;

/**
 * Repositorio MongoDB para la gestión de obligaciones tributarias.
 */
public interface TaxObligationRepository extends MongoRepository<TaxObligation, String> {

    /**
     * Lista todas las obligaciones de un contribuyente dado.
     *
     * @param taxPayerId ID del contribuyente
     * @return lista de obligaciones asociadas
     */
    List<TaxObligation> findByTaxPayerId(String taxPayerId);

    /**
     * Verifica si ya existe una obligación para un contribuyente con el mismo
     * tipo y período fiscal (restricción de unicidad compuesta).
     *
     * @param taxPayerId   ID del contribuyente
     * @param type         tipo de obligación tributaria
     * @param fiscalPeriod período fiscal
     * @return {@code true} si ya existe una obligación duplicada
     */
    boolean existsByTaxPayerIdAndTypeAndFiscalPeriod(String taxPayerId,
                                                      TaxObligationType type,
                                                      String fiscalPeriod);
}
