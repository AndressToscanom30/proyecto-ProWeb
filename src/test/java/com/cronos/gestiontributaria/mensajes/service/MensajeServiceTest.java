package com.cronos.gestiontributaria.mensajes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cronos.gestiontributaria.auth.model.User;
import com.cronos.gestiontributaria.auth.service.UserService;
import com.cronos.gestiontributaria.mensajes.model.Mensaje;
import com.cronos.gestiontributaria.mensajes.repository.MensajeRepository;

@ExtendWith(MockitoExtension.class)
class MensajeServiceTest {

    @Mock
    private MensajeRepository mensajeRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private MensajeService mensajeService;

    @Test
    void enviarMensaje_receptorExiste_guardaYRetornaMensaje() {
        User emisor = new User();
        emisor.setEmail("emisor@test.com");
        User receptor = new User();
        receptor.setEmail("receptor@test.com");

        when(userService.findByEmail("receptor@test.com")).thenReturn(Optional.of(receptor));

        Mensaje mensajeGuardado = new Mensaje("emisor@test.com", "receptor@test.com", "Asunto", "Contenido");
        mensajeGuardado.setId("msg-001");
        when(mensajeRepository.save(any(Mensaje.class))).thenReturn(mensajeGuardado);

        Mensaje resultado = mensajeService.enviarMensaje("emisor@test.com", "receptor@test.com", "Asunto", "Contenido");

        assertNotNull(resultado);
        assertEquals("msg-001", resultado.getId());
        assertEquals("emisor@test.com", resultado.getEmisorEmail());
        assertEquals("receptor@test.com", resultado.getReceptorEmail());
        assertFalse(resultado.isLeido());
        assertNotNull(resultado.getFechaEnvio());
        verify(mensajeRepository).save(any(Mensaje.class));
    }

    @Test
    void enviarMensaje_receptorNoExiste_lanzaIllegalArgument() {
        when(userService.findByEmail("no-existe@test.com")).thenReturn(Optional.empty());

        Exception ex = assertThrows(IllegalArgumentException.class,
                () -> mensajeService.enviarMensaje("emisor@test.com", "no-existe@test.com", "Asunto", "Contenido"));

        assertTrue(ex.getMessage().contains("no existe"));
        verify(mensajeRepository, never()).save(any());
    }

    @Test
    void obtenerBandejaEntrada_retornaMensajesDelReceptor() {
        Mensaje m1 = new Mensaje("otro@test.com", "user@test.com", "A1", "C1");
        Mensaje m2 = new Mensaje("otro2@test.com", "user@test.com", "A2", "C2");
        when(mensajeRepository.findByReceptorEmailOrderByFechaEnvioDesc("user@test.com"))
                .thenReturn(List.of(m1, m2));

        List<Mensaje> resultado = mensajeService.obtenerBandejaEntrada("user@test.com");

        assertEquals(2, resultado.size());
        verify(mensajeRepository).findByReceptorEmailOrderByFechaEnvioDesc("user@test.com");
    }

    @Test
    void obtenerEnviados_retornaMensajesDelEmisor() {
        Mensaje m1 = new Mensaje("user@test.com", "r1@test.com", "A1", "C1");
        when(mensajeRepository.findByEmisorEmailOrderByFechaEnvioDesc("user@test.com"))
                .thenReturn(List.of(m1));

        List<Mensaje> resultado = mensajeService.obtenerEnviados("user@test.com");

        assertEquals(1, resultado.size());
        verify(mensajeRepository).findByEmisorEmailOrderByFechaEnvioDesc("user@test.com");
    }

    @Test
    void marcarComoLeido_mensajePropio_actualizaYRetorna() {
        Mensaje mensaje = new Mensaje("emisor@test.com", "receptor@test.com", "Asunto", "Contenido");
        mensaje.setId("msg-001");
        mensaje.setLeido(false);
        when(mensajeRepository.findById("msg-001")).thenReturn(Optional.of(mensaje));
        when(mensajeRepository.save(any(Mensaje.class))).thenAnswer(inv -> inv.getArgument(0));

        Mensaje resultado = mensajeService.marcarComoLeido("msg-001", "receptor@test.com");

        assertTrue(resultado.isLeido());
        verify(mensajeRepository).save(mensaje);
    }

    @Test
    void marcarComoLeido_mensajeAjeno_lanzaSecurityException() {
        Mensaje mensaje = new Mensaje("emisor@test.com", "otro@test.com", "Asunto", "Contenido");
        mensaje.setId("msg-001");
        when(mensajeRepository.findById("msg-001")).thenReturn(Optional.of(mensaje));

        assertThrows(SecurityException.class,
                () -> mensajeService.marcarComoLeido("msg-001", "receptor@test.com"));
        verify(mensajeRepository, never()).save(any());
    }

    @Test
    void marcarComoLeido_mensajeNoExiste_lanzaNoSuchElement() {
        when(mensajeRepository.findById("msg-inexistente")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> mensajeService.marcarComoLeido("msg-inexistente", "user@test.com"));
        verify(mensajeRepository, never()).save(any());
    }

    @Test
    void contarNoLeidos_retornaCantidad() {
        when(mensajeRepository.countByReceptorEmailAndLeidoFalse("user@test.com")).thenReturn(5L);

        long count = mensajeService.contarNoLeidos("user@test.com");

        assertEquals(5L, count);
        verify(mensajeRepository).countByReceptorEmailAndLeidoFalse("user@test.com");
    }
}
