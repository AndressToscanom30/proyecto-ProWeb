package com.cronos.gestiontributaria.mensajes;

import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.cronos.gestiontributaria.mensajes.controller.MensajeController;
import com.cronos.gestiontributaria.mensajes.service.MensajeService;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas unitarias del controlador de mensajes con {@code @WebMvcTest}.
 *
 * <p>Carga únicamente la capa web (MensajeController) sin levantar el
 * contexto completo de Spring ni conectarse a la base de datos.
 * La capa de servicio se simula con un bean mock.</p>
 *
 * <p>Ejecutar con: {@code ./mvnw test -Dtest=MensajeControllerTest}</p>
 */
@WebMvcTest(MensajeController.class)
@Import(MensajeControllerTest.MockConfig.class)
class MensajeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MensajeService mensajeService;

    /**
     * Configuración interna que provee el mock de MensajeService.
     */
    @Configuration
    static class MockConfig {
        @Bean
        public MensajeService mensajeService() {
            return Mockito.mock(MensajeService.class);
        }
    }

    /**
     * Test 1: GET /api/mensajes/bandeja-entrada con usuario autenticado
     * debe retornar HTTP 200 OK.
     */
    @Test
    @DisplayName("GET /api/mensajes/bandeja-entrada con usuario autenticado → 200 OK")
    @WithMockUser(username = "usuario@test.com")
    void bandejaEntrada_conUsuarioAutenticado_retorna200() throws Exception {
        when(mensajeService.obtenerBandejaEntrada(anyString()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/mensajes/bandeja-entrada"))
                .andExpect(status().isOk());
    }

    /**
     * Test 2: GET /api/mensajes/bandeja-entrada sin autenticación
     * debe retornar HTTP 401 Unauthorized o 403 Forbidden.
     */
    @Test
    @DisplayName("GET /api/mensajes/bandeja-entrada sin autenticación → 401 o 403")
    void bandejaEntrada_sinAutenticacion_retorna401o403() throws Exception {
        mockMvc.perform(get("/api/mensajes/bandeja-entrada"))
                .andExpect(status().is4xxClientError());
    }

    /**
     * Test 3: POST /api/mensajes con cuerpo vacío o campos obligatorios faltantes
     * debe retornar HTTP 400 Bad Request.
     */
    @Test
    @DisplayName("POST /api/mensajes con cuerpo vacío → 400 Bad Request")
    @WithMockUser(username = "usuario@test.com")
    void enviarMensaje_conCuerpoVacio_retorna400() throws Exception {
        mockMvc.perform(post("/api/mensajes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
