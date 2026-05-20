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
     * Busca usuarios por el nombre exacto del rol persistido.
     *
     * @param roleName nombre persistido del rol, por ejemplo {@code ROLE_CONTADOR}
     * @return usuarios que tienen ese rol
     */
    List<User> findByRole_Name(String roleName);

    /**
     * Busca usuarios cuyos roles estén dentro de la lista recibida.
     *
     * @param roleNames nombres persistidos de los roles
     * @return usuarios con alguno de los roles indicados
     */
    List<User> findByRole_NameIn(List<String> roleNames);

    /**
     * Busca el usuario asociado a un contribuyente.
     *
     * @param taxPayerId ID del {@code TaxPayer}
     * @return usuario encontrado, si existe
     */
    Optional<User> findByTaxPayerId(String taxPayerId);
}