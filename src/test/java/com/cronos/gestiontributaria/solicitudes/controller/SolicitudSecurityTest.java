package com.cronos.gestiontributaria.solicitudes.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SolicitudSecurityTest {

    @Autowired
    private MockMvc mockMvc;

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
        assertTrue(status == 401 || status == 403 || status == 302,
                "Sin autenticación se esperaba 401/403/302, fue: " + status);
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = "USER")
    void radicar_conUsuarioAutenticado_retorna201() throws Exception {
        mockMvc.perform(post("/api/solicitudes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo": "SOPORTE", "descripcion": "Solicitud de prueba"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = "USER")
    void aprobar_comoUsuarioSinAdmin_retorna403() throws Exception {
        mockMvc.perform(put("/api/solicitudes/id-inexistente/aprobar")
                        .param("observacion", "test")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
