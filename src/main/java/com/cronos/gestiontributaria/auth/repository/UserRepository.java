package com.cronos.gestiontributaria.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.cronos.gestiontributaria.auth.model.User;

/**
 * Repositorio MongoDB para consultar usuarios por correo y ordenarlos por nombre.
 */
public interface UserRepository extends MongoRepository<User, String> {

    /**
     * Busca un usuario por su correo electrónico.
     *
     * @param email correo a consultar
     * @return usuario encontrado, si existe
     */
    Optional<User> findByEmail(String email);

    /**
     * Indica si ya existe un usuario con el correo indicado.
     *
     * @param email correo a validar
     * @return {@code true} si el correo ya está registrado
     */
    boolean existsByEmail(String email);

    /**
     * Retorna los usuarios ordenados alfabéticamente por nombre.
     *
     * @return lista de usuarios ordenados
     */
    List<User> findAllByOrderByNameAsc();

    /**
     * Busca el usuario asociado a un contribuyente.
     *
     * @param taxPayerId ID del {@code TaxPayer}
     * @return usuario encontrado, si existe
     */
    Optional<User> findByTaxPayerId(String taxPayerId);
}