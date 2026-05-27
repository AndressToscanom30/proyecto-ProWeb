package com.cronos.gestiontributaria.mensajes.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cronos.gestiontributaria.mensajes.service.MensajeService;

@Import(MensajeControllerTest.MethodSecurityTestConfig.class)
@WebMvcTest(MensajeController.class)
public class MensajeControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {}

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MensajeService mensajeService;

    // ── TEST 1 ──────────────────────────────────────────────
    // GET /api/mensajes/bandeja-entrada con usuario autenticado → 200 OK
    @Test
    @WithMockUser(username = "user@test.com")
    void getBandejaEntrada_autenticado_retorna200() throws Exception {
        mockMvc.perform(get("/api/mensajes/bandeja-entrada")
                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk());
    }

    // ── TEST 2 ──────────────────────────────────────────────
    // GET /api/mensajes/bandeja-entrada sin autenticación → 401 o 403
    @Test
    void getBandejaEntrada_sinAutenticacion_retorna401o403() throws Exception {
        mockMvc.perform(get("/api/mensajes/bandeja-entrada")
                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().is(
                   org.hamcrest.Matchers.either(
                       org.hamcrest.Matchers.is(401))
                   .or(org.hamcrest.Matchers.is(403))));
    }

    // ── TEST 3 ──────────────────────────────────────────────
    // POST /api/mensajes con body vacío → 400 Bad Request
    @Test
    @WithMockUser(username = "user@test.com")
    void enviarMensaje_bodyVacio_retorna400() throws Exception {
        mockMvc.perform(post("/api/mensajes")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
               .andExpect(status().isBadRequest());
    }
}
