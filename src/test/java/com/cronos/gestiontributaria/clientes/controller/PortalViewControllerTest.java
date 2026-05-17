package com.cronos.gestiontributaria.clientes.controller;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.cronos.gestiontributaria.auth.service.UserService;
import com.cronos.gestiontributaria.clientes.service.TaxPayerService;
import com.cronos.gestiontributaria.obligaciones.service.DocumentService;
import com.cronos.gestiontributaria.obligaciones.service.TaxObligationService;

/**
 * Tests del slice MVC del {@link PortalViewController}.
 *
 * <p>Se evita importar el {@code SecurityConfig} real para no arrastrar
 * {@code CustomUserDetailsService}/{@code UserRepository}. La configuración
 * por defecto de Spring Security exige autenticación; {@code @WithMockUser}
 * la suple. Se importa {@link MethodSecurityTestConfig} para activar
 * {@code @PreAuthorize} también en el contexto de test.</p>
 */
@WebMvcTest(PortalViewController.class)
@Import(PortalViewControllerTest.MethodSecurityTestConfig.class)
class PortalViewControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private UserService userService;
    @MockitoBean private TaxPayerService taxPayerService;
    @MockitoBean private TaxObligationService taxObligationService;
    @MockitoBean private DocumentService documentService;

    @Test
    @WithMockUser(roles = "CONTRIBUYENTE")
    void portalRaiz_comoContribuyente_redirigeAInicio() throws Exception {
        mockMvc.perform(get("/portal"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/portal/inicio"));
    }

    @Test
    @WithMockUser(roles = "CONTRIBUYENTE")
    void portalInicio_comoContribuyente_retorna200() throws Exception {
        mockMvc.perform(get("/portal/inicio"))
                .andExpect(status().isOk())
                .andExpect(view().name("portal/inicio"));
    }

    @Test
    @WithMockUser(roles = "CONTRIBUYENTE")
    void portalObligaciones_comoContribuyente_retorna200() throws Exception {
        mockMvc.perform(get("/portal/obligaciones"))
                .andExpect(status().isOk())
                .andExpect(view().name("portal/obligaciones"));
    }

    @Test
    void portalInicio_sinAutenticacion_noPermiteAcceso() throws Exception {
        MvcResult result = mockMvc.perform(get("/portal/inicio")).andReturn();
        int status = result.getResponse().getStatus();
        // Aceptamos 401, 403 o redirect a login (302). Cualquier cosa
        // distinta a 200 indica que se negó el acceso.
        assertNotEquals(200, status,
                "Sin autenticación NO debe responder 200, fue: " + status);
    }
}
