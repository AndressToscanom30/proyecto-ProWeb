package com.cronos.gestiontributaria.solicitudes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración de seguridad para el módulo de solicitudes.
 *
 * <p>Verifica el comportamiento del sistema ante diferentes roles de usuario
 * usando {@code @SpringBootTest} (contexto completo) y {@code @WithMockUser}
 * para simular usuarios con roles específicos.</p>
 *
 * <p>Ejecutar con: {@code mvn test -Dtest=SolicitudSecurityTest}</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class SolicitudSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Test 1: POST /api/solicitudes sin autenticación debe retornar
     * HTTP 401 Unauthorized o 403 Forbidden.
     */
    @Test
    @DisplayName("POST /api/solicitudes sin autenticación → acceso denegado (redirección a login)")
    void crearSolicitud_sinAutenticacion_retorna401o403() throws Exception {
        String body = "{\"tipo\": \"SOPORTE\", \"descripcion\": \"Necesito ayuda\"}";

        int status = mockMvc.perform(post("/api/solicitudes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getStatus();

        // Con formLogin, Spring redirige al login (302) en vez de 401/403.
        // Cualquier respuesta distinta a 200/201 indica acceso denegado.
        assert status == 302 || status == 401 || status == 403
                : "Se esperaba 302, 401 o 403 pero fue: " + status;
    }

    /**
     * Test 2: POST /api/solicitudes con usuario autenticado sin rol especial
     * debe retornar HTTP 201 Created.
     */
    @Test
    @DisplayName("POST /api/solicitudes con usuario autenticado → 201 Created")
    @WithMockUser(username = "usuario@test.com", roles = "USER")
    void crearSolicitud_conUsuarioAutenticado_retorna201() throws Exception {
        String body = "{\"tipo\": \"SOPORTE\", \"descripcion\": \"Necesito soporte tecnico\"}";

        mockMvc.perform(post("/api/solicitudes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    /**
     * Test 3: PUT /api/solicitudes/{id}/aprobar con usuario con rol USER
     * (sin ADMIN) debe retornar HTTP 403 Forbidden.
     *
     * <p>Se acepta que el ID de solicitud no exista — el sistema debe
     * retornar 403 sin llegar a buscar el registro, ya que la restricción
     * de seguridad se evalúa antes de la lógica de negocio.</p>
     */
    @Test
    @DisplayName("PUT /api/solicitudes/{id}/aprobar con rol USER → 403 Forbidden")
    @WithMockUser(username = "usuario@test.com", roles = "USER")
    void aprobarSolicitud_conRolUser_retorna403() throws Exception {
        mockMvc.perform(put("/api/solicitudes/999/aprobar")
                        .param("observacion", "Aprobado por prueba"))
                .andExpect(status().isForbidden());
    }
}
