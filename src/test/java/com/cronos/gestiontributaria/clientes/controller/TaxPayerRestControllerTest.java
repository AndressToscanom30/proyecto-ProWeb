package com.cronos.gestiontributaria.clientes.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.cronos.gestiontributaria.auth.model.User;
import com.cronos.gestiontributaria.auth.service.UserService;
import com.cronos.gestiontributaria.clientes.model.TaxPayer;
import com.cronos.gestiontributaria.clientes.service.TaxPayerService;
import com.cronos.gestiontributaria.obligaciones.dto.TaxObligationResponseDTO;
import com.cronos.gestiontributaria.obligaciones.model.Document;
import com.cronos.gestiontributaria.obligaciones.service.DocumentService;
import com.cronos.gestiontributaria.obligaciones.service.TaxObligationService;

/**
 * Tests del slice MVC del {@link TaxPayerRestController}.
 *
 * <p>No se importa {@code SecurityConfig} real para evitar arrastrar
 * dependencias de {@code CustomUserDetailsService} y {@code UserRepository}.
 * {@code @WebMvcTest} aplica la configuración por defecto de Spring Security
 * que exige autenticación; {@code @WithMockUser} suple esa autenticación
 * para los casos felices.</p>
 */
@WebMvcTest(TaxPayerRestController.class)
class TaxPayerRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaxPayerService taxPayerService;

    @MockitoBean
    private TaxObligationService taxObligationService;

    @MockitoBean
    private DocumentService documentService;

    @MockitoBean
    private UserService userService;

    private User userConTaxPayer(String email, String taxPayerId) {
        User u = new User();
        u.setEmail(email);
        u.setTaxPayerId(taxPayerId);
        return u;
    }

    @Test
    @WithMockUser(username = "contribuyente@test.com", roles = "CONTRIBUYENTE")
    void getMiPerfil_autenticado_retorna200() throws Exception {
        when(userService.findByEmail("contribuyente@test.com"))
                .thenReturn(Optional.of(userConTaxPayer("contribuyente@test.com", "tp-001")));
        TaxPayer tp = new TaxPayer();
        tp.setId("tp-001");
        tp.setBusinessName("Empresa Demo");
        when(taxPayerService.findById("tp-001")).thenReturn(tp);

        mockMvc.perform(get("/api/contribuyente/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("tp-001"))
                .andExpect(jsonPath("$.businessName").value("Empresa Demo"));
    }

    @Test
    @WithMockUser(username = "sin-vinculo@test.com", roles = "CONTRIBUYENTE")
    void getMiPerfil_sinTaxPayerId_retorna403() throws Exception {
        User user = new User();
        user.setEmail("sin-vinculo@test.com");
        user.setTaxPayerId(null);
        when(userService.findByEmail("sin-vinculo@test.com"))
                .thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/contribuyente/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "contribuyente@test.com", roles = "CONTRIBUYENTE")
    void getMisObligaciones_retornaLista() throws Exception {
        when(userService.findByEmail("contribuyente@test.com"))
                .thenReturn(Optional.of(userConTaxPayer("contribuyente@test.com", "tp-001")));
        TaxObligationResponseDTO o1 = new TaxObligationResponseDTO(
                "ob-1", "tp-001", "Empresa", "900", null, null, "2026-01", 2026,
                null, false, null, null, null);
        TaxObligationResponseDTO o2 = new TaxObligationResponseDTO(
                "ob-2", "tp-001", "Empresa", "900", null, null, "2026-02", 2026,
                null, false, null, null, null);
        when(taxObligationService.findByTaxPayerId("tp-001"))
                .thenReturn(List.of(o1, o2));

        mockMvc.perform(get("/api/contribuyente/me/obligaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @WithMockUser(username = "contribuyente@test.com", roles = "CONTRIBUYENTE")
    void getMisDocumentos_retornaLista() throws Exception {
        when(userService.findByEmail("contribuyente@test.com"))
                .thenReturn(Optional.of(userConTaxPayer("contribuyente@test.com", "tp-001")));
        when(documentService.findByTaxPayer("tp-001"))
                .thenReturn(List.of(new Document(), new Document(), new Document()));

        mockMvc.perform(get("/api/contribuyente/me/documentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void getMiPerfil_sinAutenticacion_retorna401o403() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/contribuyente/me")).andReturn();
        int status = result.getResponse().getStatus();
        // Spring Security puede devolver 401 (sin auth entry point claro) o
        // 403, o un 302 si hay redirección a login. Cualquiera niega el acceso.
        assertTrue(status == 401 || status == 403 || status == 302,
                "Sin autenticación se esperaba 401/403/302, fue: " + status);
    }
}
