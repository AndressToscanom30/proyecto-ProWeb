package com.cronos.gestiontributaria.mensajes.controller;

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

import com.cronos.gestiontributaria.mensajes.model.Mensaje;
import com.cronos.gestiontributaria.mensajes.service.MensajeService;

@WebMvcTest(MensajeController.class)
@Import(MensajeControllerTest.MethodSecurityTestConfig.class)
class MensajeControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MensajeService mensajeService;

    private Mensaje mensajeEjemplo(String id, String emisor, String receptor) {
        Mensaje m = new Mensaje(emisor, receptor, "Asunto prueba", "Contenido prueba");
        m.setId(id);
        m.setFechaEnvio(LocalDateTime.now());
        return m;
    }

    // ─── POST /api/mensajes ───────────────────────────────────────────────

    @Test
    @WithMockUser(username = "emisor@test.com", roles = "GERENTE")
    void enviarMensaje_valido_retorna201() throws Exception {
        Mensaje msg = mensajeEjemplo("msg-001", "emisor@test.com", "receptor@test.com");
        when(mensajeService.enviarMensaje("emisor@test.com", "receptor@test.com", "Asunto", "Contenido"))
                .thenReturn(msg);

        mockMvc.perform(post("/api/mensajes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "receptorEmail": "receptor@test.com",
                                    "asunto": "Asunto",
                                    "contenido": "Contenido"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("msg-001"))
                .andExpect(jsonPath("$.emisorEmail").value("emisor@test.com"))
                .andExpect(jsonPath("$.receptorEmail").value("receptor@test.com"));
    }

    @Test
    @WithMockUser(username = "emisor@test.com", roles = "GERENTE")
    void enviarMensaje_receptorNoExiste_retorna400() throws Exception {
        when(mensajeService.enviarMensaje(eq("emisor@test.com"), eq("no-existe@test.com"), any(), any()))
                .thenThrow(new IllegalArgumentException("El usuario receptor con correo no-existe@test.com no existe"));

        mockMvc.perform(post("/api/mensajes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "receptorEmail": "no-existe@test.com",
                                    "asunto": "Asunto",
                                    "contenido": "Contenido"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "emisor@test.com", roles = "GERENTE")
    void enviarMensaje_camposVacios_retorna400() throws Exception {
        mockMvc.perform(post("/api/mensajes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "receptorEmail": "",
                                    "asunto": "",
                                    "contenido": ""
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(mensajeService, never()).enviarMensaje(any(), any(), any(), any());
    }

    @Test
    void enviarMensaje_sinAutenticacion_retorna401o403() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/mensajes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "receptorEmail": "r@test.com",
                                    "asunto": "A",
                                    "contenido": "C"
                                }
                                """))
                .andReturn();
        int status = result.getResponse().getStatus();
        assertTrue(status == 401 || status == 403 || status == 302,
                "Sin autenticación se esperaba 401/403/302, fue: " + status);
    }

    // ─── GET /api/mensajes/bandeja-entrada ────────────────────────────────

    @Test
    @WithMockUser(username = "user@test.com", roles = "GERENTE")
    void bandejaEntrada_retornaLista() throws Exception {
        when(mensajeService.obtenerBandejaEntrada("user@test.com"))
                .thenReturn(List.of(
                        mensajeEjemplo("msg-1", "otro@test.com", "user@test.com"),
                        mensajeEjemplo("msg-2", "otro2@test.com", "user@test.com")));

        mockMvc.perform(get("/api/mensajes/bandeja-entrada"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].id").value("msg-1"))
                .andExpect(jsonPath("$[1].id").value("msg-2"));
    }

    @Test
    void bandejaEntrada_sinAutenticacion_retorna401o403() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/mensajes/bandeja-entrada")).andReturn();
        int status = result.getResponse().getStatus();
        assertTrue(status == 401 || status == 403 || status == 302,
                "Sin autenticación se esperaba 401/403/302, fue: " + status);
    }

    // ─── GET /api/mensajes/enviados ───────────────────────────────────────

    @Test
    @WithMockUser(username = "user@test.com", roles = "GERENTE")
    void enviados_retornaLista() throws Exception {
        when(mensajeService.obtenerEnviados("user@test.com"))
                .thenReturn(List.of(
                        mensajeEjemplo("msg-1", "user@test.com", "r1@test.com")));

        mockMvc.perform(get("/api/mensajes/enviados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].emisorEmail").value("user@test.com"));
    }

    // ─── PUT /api/mensajes/{id}/leer ──────────────────────────────────────

    @Test
    @WithMockUser(username = "receptor@test.com", roles = "GERENTE")
    void marcarLeido_mensajePropio_retorna200() throws Exception {
        Mensaje leido = mensajeEjemplo("msg-001", "emisor@test.com", "receptor@test.com");
        leido.setLeido(true);
        when(mensajeService.marcarComoLeido("msg-001", "receptor@test.com")).thenReturn(leido);

        mockMvc.perform(put("/api/mensajes/msg-001/leer").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leido").value(true));
    }

    @Test
    @WithMockUser(username = "receptor@test.com", roles = "GERENTE")
    void marcarLeido_mensajeNoExiste_retorna404() throws Exception {
        when(mensajeService.marcarComoLeido("msg-inexistente", "receptor@test.com"))
                .thenThrow(new NoSuchElementException("Mensaje no encontrado con ID: msg-inexistente"));

        mockMvc.perform(put("/api/mensajes/msg-inexistente/leer").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "otro@test.com", roles = "GERENTE")
    void marcarLeido_mensajeAjeno_retorna403() throws Exception {
        when(mensajeService.marcarComoLeido("msg-001", "otro@test.com"))
                .thenThrow(new SecurityException("No puedes marcar como leído un mensaje que no te pertenece"));

        mockMvc.perform(put("/api/mensajes/msg-001/leer").with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ─── GET /api/mensajes/no-leidos/count ────────────────────────────────

    @Test
    @WithMockUser(username = "user@test.com", roles = "GERENTE")
    void contarNoLeidos_retornaCount() throws Exception {
        when(mensajeService.contarNoLeidos("user@test.com")).thenReturn(3L);

        mockMvc.perform(get("/api/mensajes/no-leidos/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));
    }
}
