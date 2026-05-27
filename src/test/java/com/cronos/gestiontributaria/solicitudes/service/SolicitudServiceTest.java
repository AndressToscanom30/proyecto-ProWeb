package com.cronos.gestiontributaria.solicitudes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cronos.gestiontributaria.solicitudes.model.EstadoSolicitud;
import com.cronos.gestiontributaria.solicitudes.model.Solicitud;
import com.cronos.gestiontributaria.solicitudes.model.TipoSolicitud;
import com.cronos.gestiontributaria.solicitudes.repository.SolicitudRepository;

@ExtendWith(MockitoExtension.class)
class SolicitudServiceTest {

    @Mock
    private SolicitudRepository solicitudRepository;

    @InjectMocks
    private SolicitudService solicitudService;

    @Test
    void radicar_creaConEstadoPendiente() {
        Solicitud guardada = new Solicitud("user@test.com", TipoSolicitud.SOPORTE, "Descripción");
        guardada.setId("sol-001");
        when(solicitudRepository.save(any(Solicitud.class))).thenReturn(guardada);

        Solicitud resultado = solicitudService.radicar("user@test.com", TipoSolicitud.SOPORTE, "Descripción");

        assertNotNull(resultado);
        assertEquals("sol-001", resultado.getId());
        assertEquals(EstadoSolicitud.PENDIENTE, resultado.getEstado());
        assertNotNull(resultado.getFechaCreacion());
        verify(solicitudRepository).save(any(Solicitud.class));
    }

    @Test
    void obtenerMisSolicitudes_retornaLista() {
        Solicitud s1 = new Solicitud("user@test.com", TipoSolicitud.ACCESO, "A");
        Solicitud s2 = new Solicitud("user@test.com", TipoSolicitud.INFORMACIÓN, "B");
        when(solicitudRepository.findBySolicitanteEmailOrderByFechaCreacionDesc("user@test.com"))
                .thenReturn(List.of(s1, s2));

        List<Solicitud> resultado = solicitudService.obtenerMisSolicitudes("user@test.com");

        assertEquals(2, resultado.size());
    }

    @Test
    void obtenerTodas_retornaLista() {
        when(solicitudRepository.findAllByOrderByFechaCreacionDesc())
                .thenReturn(List.of(new Solicitud(), new Solicitud()));

        List<Solicitud> resultado = solicitudService.obtenerTodas();

        assertEquals(2, resultado.size());
    }

    @Test
    void aprobar_solicitudPendiente_cambiaEstado() {
        Solicitud solicitud = new Solicitud("user@test.com", TipoSolicitud.SOPORTE, "Desc");
        solicitud.setId("sol-001");
        when(solicitudRepository.findById("sol-001")).thenReturn(Optional.of(solicitud));
        when(solicitudRepository.save(any(Solicitud.class))).thenAnswer(inv -> inv.getArgument(0));

        Solicitud resultado = solicitudService.aprobar("sol-001", "Aprobado");

        assertEquals(EstadoSolicitud.APROBADA, resultado.getEstado());
        assertEquals("Aprobado", resultado.getObservacion());
        assertNotNull(resultado.getFechaResolucion());
        verify(solicitudRepository).save(solicitud);
    }

    @Test
    void aprobar_solicitudYaResuelta_lanzaIllegalState() {
        Solicitud solicitud = new Solicitud("user@test.com", TipoSolicitud.SOPORTE, "Desc");
        solicitud.setId("sol-001");
        solicitud.setEstado(EstadoSolicitud.APROBADA);
        when(solicitudRepository.findById("sol-001")).thenReturn(Optional.of(solicitud));

        assertThrows(IllegalStateException.class,
                () -> solicitudService.aprobar("sol-001", "observación"));
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void aprobar_solicitudRechazada_lanzaIllegalState() {
        Solicitud solicitud = new Solicitud("user@test.com", TipoSolicitud.SOPORTE, "Desc");
        solicitud.setId("sol-001");
        solicitud.setEstado(EstadoSolicitud.RECHAZADA);
        when(solicitudRepository.findById("sol-001")).thenReturn(Optional.of(solicitud));

        assertThrows(IllegalStateException.class,
                () -> solicitudService.aprobar("sol-001", "observación"));
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void rechazar_solicitudPendiente_cambiaEstado() {
        Solicitud solicitud = new Solicitud("user@test.com", TipoSolicitud.SOPORTE, "Desc");
        solicitud.setId("sol-001");
        when(solicitudRepository.findById("sol-001")).thenReturn(Optional.of(solicitud));
        when(solicitudRepository.save(any(Solicitud.class))).thenAnswer(inv -> inv.getArgument(0));

        Solicitud resultado = solicitudService.rechazar("sol-001", "Rechazado");

        assertEquals(EstadoSolicitud.RECHAZADA, resultado.getEstado());
        assertEquals("Rechazado", resultado.getObservacion());
        assertNotNull(resultado.getFechaResolucion());
        verify(solicitudRepository).save(solicitud);
    }

    @Test
    void rechazar_solicitudYaAprobada_lanzaIllegalState() {
        Solicitud solicitud = new Solicitud("user@test.com", TipoSolicitud.SOPORTE, "Desc");
        solicitud.setId("sol-001");
        solicitud.setEstado(EstadoSolicitud.APROBADA);
        when(solicitudRepository.findById("sol-001")).thenReturn(Optional.of(solicitud));

        assertThrows(IllegalStateException.class,
                () -> solicitudService.rechazar("sol-001", "observación"));
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void resolver_solicitudNoExiste_lanzaNoSuchElement() {
        when(solicitudRepository.findById("sol-inexistente")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> solicitudService.aprobar("sol-inexistente", "obs"));
        assertThrows(NoSuchElementException.class,
                () -> solicitudService.rechazar("sol-inexistente", "obs"));
        verify(solicitudRepository, never()).save(any());
    }
}
