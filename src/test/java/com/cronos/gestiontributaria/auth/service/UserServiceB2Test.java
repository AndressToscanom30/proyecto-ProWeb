package com.cronos.gestiontributaria.auth.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.cronos.gestiontributaria.auth.model.Role;
import com.cronos.gestiontributaria.auth.model.User;
import com.cronos.gestiontributaria.auth.repository.UserRepository;

/**
 * Tests para los cambios introducidos en el paso B-2 sobre {@link UserService}:
 *  - vinculación de un User al rol CONTRIBUYENTE y a un TaxPayer.
 *  - existencia de la consulta {@code findByTaxPayerId} en el repositorio.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceB2Test {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User nuevoGerente(String email) {
        User u = new User();
        u.setName("Juan");
        u.setEmail(email);
        u.setPasswordHash("hash123");
        u.setActive(true);
        u.setRole(new Role("ROLE_GERENTE", "Gerente", new ArrayList<>()));
        return u;
    }

    @Test
    void vincularContribuyente_asignaRolYTaxPayerId() {
        User existing = nuevoGerente("test@test.com");
        when(userRepository.findByEmail("test@test.com"))
                .thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        userService.vincularContribuyente("test@test.com", "tp-001");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals(Role.CONTRIBUYENTE, saved.getRole().getName(),
                "El rol debe ser ROLE_CONTRIBUYENTE");
        assertEquals("tp-001", saved.getTaxPayerId());
    }

    @Test
    void vincularContribuyente_emailInexistente_noLanzaExcepcion() {
        when(userRepository.findByEmail("ghost@test.com"))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() ->
                userService.vincularContribuyente("ghost@test.com", "tp-002"));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void vincularContribuyente_noModificaOtrosCampos() {
        User existing = nuevoGerente("juan@test.com");
        existing.setName("juan");
        existing.setPasswordHash("hash123");
        when(userRepository.findByEmail("juan@test.com"))
                .thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        userService.vincularContribuyente("juan@test.com", "tp-003");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("juan", saved.getName(),
                "El nombre no debe cambiar al vincular");
        assertEquals("hash123", saved.getPasswordHash(),
                "El passwordHash no debe cambiar al vincular");
    }

    @Test
    void findByTaxPayerId_retornaUserCorrecto() {
        User user = new User();
        user.setEmail("contrib@test.com");
        user.setTaxPayerId("tp-999");
        when(userRepository.findByTaxPayerId("tp-999"))
                .thenReturn(Optional.of(user));

        Optional<User> result = userRepository.findByTaxPayerId("tp-999");

        assertTrue(result.isPresent(), "Debe encontrar el User");
        assertEquals("tp-999", result.get().getTaxPayerId());
        assertEquals("contrib@test.com", result.get().getEmail());
    }
}
