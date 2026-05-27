package com.cronos.gestiontributaria.solicitudes.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.cronos.gestiontributaria.solicitudes.model.EstadoSolicitud;
import com.cronos.gestiontributaria.solicitudes.model.Solicitud;
import com.cronos.gestiontributaria.solicitudes.model.TipoSolicitud;
import com.cronos.gestiontributaria.solicitudes.service.SolicitudService;

@WebMvcTest(SolicitudController.class)
@Import(SolicitudControllerTest.MethodSecurityTestConfig.class)
class SolicitudControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SolicitudService solicitudService;

    private Solicitud solicitudEjemplo(String id, String email, TipoSolicitud tipo, EstadoSolicitud estado) {
        Solicitud s = new Solicitud(email, tipo, "Descripción de prueba");
        s.setId(id);
        s.setEstado(estado);
        s.setFechaCreacion(LocalDateTime.now());
        return s;
    }

    // ─── POST /api/solicitudes ────────────────────────────────────────────

    @Test
    @WithMockUser(username = "user@test.com", roles = "GERENTE")
    void radicar_valido_retorna201() throws Exception {
        Solicitud sol = solicitudEjemplo("sol-001", "user@test.com", TipoSolicitud.SOPORTE, EstadoSolicitud.PENDIENTE);
        when(solicitudService.radicar("user@test.com", TipoSolicitud.SOPORTE, "Descripción")).thenReturn(sol);

        mockMvc.perform(post("/api/solicitudes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "tipo": "SOPORTE",
                                    "descripcion": "Descripción"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("sol-001"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = "GERENTE")
    void radicar_camposInvalidos_retorna400() throws Exception {
        mockMvc.perform(post("/api/solicitudes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "tipo": null,
                                    "descripcion": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
        verify(solicitudService, never()).radicar(any(), any(), any());
    }

    @Test
    void radicar_sinAutenticacion_retorna401o403() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/solicitudes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo": "SOPORTE", "descripcion": "Test"}
                                """))
                .andReturn();
        int status = result.getResponse().getStatus();
        assertTrue(status == 401 || status == 403 || status == 302);
    }

    // ─── GET /api/solicitudes/mis-solicitudes ─────────────────────────────

    @Test
    @WithMockUser(username = "user@test.com", roles = "GERENTE")
    void misSolicitudes_retornaLista() throws Exception {
        when(solicitudService.obtenerMisSolicitudes("user@test.com"))
                .thenReturn(List.of(
                        solicitudEjemplo("sol-1", "user@test.com", TipoSolicitud.ACCESO, EstadoSolicitud.PENDIENTE)));

        mockMvc.perform(get("/api/solicitudes/mis-solicitudes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(1)));
    }

    // ─── GET /api/solicitudes ─────────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void obtenerTodas_comoAdmin_retorna200() throws Exception {
        when(solicitudService.obtenerTodas())
                .thenReturn(List.of(
                        solicitudEjemplo("sol-1", "u1@test.com", TipoSolicitud.SOPORTE, EstadoSolicitud.PENDIENTE),
                        solicitudEjemplo("sol-2", "u2@test.com", TipoSolicitud.INFORMACIÓN, EstadoSolicitud.APROBADA)));

        mockMvc.perform(get("/api/solicitudes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(2)));
    }

    @Test
    @WithMockUser(username = "gerente@test.com", roles = "GERENTE")
    void obtenerTodas_comoNoAdmin_retorna403() throws Exception {
        mockMvc.perform(get("/api/solicitudes"))
                .andExpect(status().isForbidden());
        verify(solicitudService, never()).obtenerTodas();
    }

    @Test
    void obtenerTodas_sinAutenticacion_retorna401o403() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/solicitudes")).andReturn();
        int status = result.getResponse().getStatus();
        assertTrue(status == 401 || status == 403 || status == 302);
    }

    // ─── PUT /api/solicitudes/{id}/aprobar ────────────────────────────────

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void aprobar_valido_retorna200() throws Exception {
        Solicitud aprobada = solicitudEjemplo("sol-001", "user@test.com", TipoSolicitud.SOPORTE, EstadoSolicitud.APROBADA);
        aprobada.setObservacion("Procede");
        when(solicitudService.aprobar("sol-001", "Procede")).thenReturn(aprobada);

        mockMvc.perform(put("/api/solicitudes/sol-001/aprobar")
                        .param("observacion", "Procede")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADA"))
                .andExpect(jsonPath("$.observacion").value("Procede"));
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void aprobar_noExiste_retorna404() throws Exception {
        when(solicitudService.aprobar("sol-inexistente", "obs"))
                .thenThrow(new NoSuchElementException("Solicitud no encontrada"));

        mockMvc.perform(put("/api/solicitudes/sol-inexistente/aprobar")
                        .param("observacion", "obs")
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void aprobar_yaResuelta_retorna400() throws Exception {
        when(solicitudService.aprobar("sol-001", "obs"))
                .thenThrow(new IllegalStateException("La solicitud ya fue aprobada"));

        mockMvc.perform(put("/api/solicitudes/sol-001/aprobar")
                        .param("observacion", "obs")
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "gerente@test.com", roles = "GERENTE")
    void aprobar_comoNoAdmin_retorna403() throws Exception {
        mockMvc.perform(put("/api/solicitudes/sol-001/aprobar")
                        .param("observacion", "obs")
                        .with(csrf()))
                .andExpect(status().isForbidden());
        verify(solicitudService, never()).aprobar(any(), any());
    }

    // ─── PUT /api/solicitudes/{id}/rechazar ───────────────────────────────

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void rechazar_valido_retorna200() throws Exception {
        Solicitud rechazada = solicitudEjemplo("sol-001", "user@test.com", TipoSolicitud.SOPORTE, EstadoSolicitud.RECHAZADA);
        rechazada.setObservacion("No procede");
        when(solicitudService.rechazar("sol-001", "No procede")).thenReturn(rechazada);

        mockMvc.perform(put("/api/solicitudes/sol-001/rechazar")
                        .param("observacion", "No procede")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADA"))
                .andExpect(jsonPath("$.observacion").value("No procede"));
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void rechazar_noExiste_retorna404() throws Exception {
        when(solicitudService.rechazar("sol-inexistente", "obs"))
                .thenThrow(new NoSuchElementException("Solicitud no encontrada"));

        mockMvc.perform(put("/api/solicitudes/sol-inexistente/rechazar")
                        .param("observacion", "obs")
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "gerente@test.com", roles = "GERENTE")
    void rechazar_comoNoAdmin_retorna403() throws Exception {
        mockMvc.perform(put("/api/solicitudes/sol-001/rechazar")
                        .param("observacion", "obs")
                        .with(csrf()))
                .andExpect(status().isForbidden());
        verify(solicitudService, never()).rechazar(any(), any());
    }
}
