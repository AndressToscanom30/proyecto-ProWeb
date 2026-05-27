package com.cronos.gestiontributaria.solicitudes.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SolicitudSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    // ── TEST 1 ──────────────────────────────────────────────
    // POST /api/solicitudes sin autenticación → 401 o 403
    @Test
    void radicarSolicitud_sinAutenticacion_retorna401o403() throws Exception {
        String body = """
                {
                  "tipo": "SOPORTE",
                  "descripcion": "Necesito soporte técnico"
                }
                """;

        mockMvc.perform(post("/api/solicitudes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
               .andExpect(status().is(
                   Matchers.either(
                       Matchers.is(401))
                   .or(Matchers.is(403))
                   .or(Matchers.is(302))));
    }

    // ── TEST 2 ──────────────────────────────────────────────
    // POST /api/solicitudes con usuario autenticado (rol USER) → 201 Created
    @Test
    @WithMockUser(roles = "USER")
    void radicarSolicitud_usuarioAutenticado_retorna201() throws Exception {
        String body = """
                {
                  "tipo": "ACCESO",
                  "descripcion": "Solicito acceso al módulo de reportes"
                }
                """;

        mockMvc.perform(post("/api/solicitudes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
               .andExpect(status().isCreated());
    }

    // ── TEST 3 ──────────────────────────────────────────────
    // PUT /api/solicitudes/{id}/aprobar con rol USER (sin ADMIN) → 403 Forbidden
    // El ID no existe, pero la respuesta debe ser 403 antes de ejecutar lógica.
    @Test
    @WithMockUser(roles = "USER")
    void aprobarSolicitud_sinRolAdmin_retorna403() throws Exception {
        mockMvc.perform(put("/api/solicitudes/id-inexistente/aprobar")
                .param("observacion", "aprobado")
                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isForbidden());
    }
}
