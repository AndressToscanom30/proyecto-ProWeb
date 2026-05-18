package com.cronos.gestiontributaria.clientes.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import java.util.Optional;

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

import com.cronos.gestiontributaria.auth.model.User;
import com.cronos.gestiontributaria.auth.service.UserService;
import com.cronos.gestiontributaria.clientes.model.TaxPayer;
import com.cronos.gestiontributaria.clientes.service.TaxPayerService;
import com.cronos.gestiontributaria.common.TaxObligationStatus;
import com.cronos.gestiontributaria.common.TaxObligationType;
import com.cronos.gestiontributaria.obligaciones.dto.TaxObligationResponseDTO;
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
    @WithMockUser(username = "user", roles = "CONTRIBUYENTE")
    void portalInicio_comoContribuyente_retorna200() throws Exception {
        when(userService.findByEmail("user"))
                .thenReturn(Optional.of(userConTaxPayer("user", "tp-001")));
        when(taxPayerService.findById("tp-001"))
                .thenReturn(ejemploTaxPayer("tp-001", "Empresa"));

        mockMvc.perform(get("/portal/inicio"))
                .andExpect(status().isOk())
                .andExpect(view().name("portal/inicio"));
    }

    @Test
    @WithMockUser(username = "user", roles = "CONTRIBUYENTE")
    void portalObligaciones_comoContribuyente_retorna200() throws Exception {
        when(userService.findByEmail("user"))
                .thenReturn(Optional.of(userConTaxPayer("user", "tp-001")));
        when(taxObligationService.findByTaxPayerId("tp-001"))
                .thenReturn(List.of());

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

    // ─── Helpers ────────────────────────────────────────────────────────

    private User userConTaxPayer(String email, String taxPayerId) {
        User u = new User();
        u.setEmail(email);
        u.setTaxPayerId(taxPayerId);
        return u;
    }

    private TaxPayer ejemploTaxPayer(String id, String businessName) {
        TaxPayer tp = new TaxPayer();
        tp.setId(id);
        tp.setBusinessName(businessName);
        tp.setIdentificacion("900100200-1");
        tp.setEmail("contacto@empresa.com");
        tp.setActive(true);
        return tp;
    }

    private TaxObligationResponseDTO ejemploObligacion(String id) {
        return new TaxObligationResponseDTO(
                id, "tp-001", "Empresa Test S.A.S", "900100200-1",
                null, TaxObligationType.INCOME_TAX, "2026", 2026,
                null, false, null, TaxObligationStatus.PENDING, null);
    }

    // ─── D-2: vista de perfil ───────────────────────────────────────────

    @Test
    @WithMockUser(username = "contribuyente@test.com", roles = "CONTRIBUYENTE")
    void inicio_pueblaModelConContribuyente() throws Exception {
        when(userService.findByEmail("contribuyente@test.com"))
                .thenReturn(Optional.of(userConTaxPayer("contribuyente@test.com", "tp-001")));
        when(taxPayerService.findById("tp-001"))
                .thenReturn(ejemploTaxPayer("tp-001", "Empresa Test S.A.S"));

        mockMvc.perform(get("/portal/inicio"))
                .andExpect(status().isOk())
                .andExpect(view().name("portal/inicio"))
                .andExpect(model().attributeExists("contribuyente"))
                .andExpect(model().attribute("contribuyente",
                        org.hamcrest.Matchers.hasProperty("businessName",
                                org.hamcrest.Matchers.equalTo("Empresa Test S.A.S"))));
    }

    @Test
    @WithMockUser(username = "sin-vinculo@test.com", roles = "CONTRIBUYENTE")
    void inicio_taxPayerIdNull_retorna403() throws Exception {
        User user = new User();
        user.setEmail("sin-vinculo@test.com");
        user.setTaxPayerId(null);
        when(userService.findByEmail("sin-vinculo@test.com"))
                .thenReturn(Optional.of(user));

        mockMvc.perform(get("/portal/inicio"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "contribuyente@test.com", roles = "CONTRIBUYENTE")
    void inicio_renderizaBusinessNameEnHtml() throws Exception {
        when(userService.findByEmail("contribuyente@test.com"))
                .thenReturn(Optional.of(userConTaxPayer("contribuyente@test.com", "tp-001")));
        when(taxPayerService.findById("tp-001"))
                .thenReturn(ejemploTaxPayer("tp-001", "Empresa Test S.A.S"));

        mockMvc.perform(get("/portal/inicio"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Empresa Test S.A.S")));
    }

    // ─── D-3: vista de obligaciones ─────────────────────────────────────

    @Test
    @WithMockUser(username = "contribuyente@test.com", roles = "CONTRIBUYENTE")
    void obligaciones_pueblaModelConLista() throws Exception {
        when(userService.findByEmail("contribuyente@test.com"))
                .thenReturn(Optional.of(userConTaxPayer("contribuyente@test.com", "tp-001")));
        when(taxObligationService.findByTaxPayerId("tp-001"))
                .thenReturn(List.of(ejemploObligacion("ob-1"), ejemploObligacion("ob-2")));

        mockMvc.perform(get("/portal/obligaciones"))
                .andExpect(status().isOk())
                .andExpect(view().name("portal/obligaciones"))
                .andExpect(model().attributeExists("obligaciones"))
                .andExpect(model().attribute("obligaciones", hasSize(2)));
    }

    @Test
    @WithMockUser(username = "contribuyente@test.com", roles = "CONTRIBUYENTE")
    void obligaciones_listaVacia_retorna200() throws Exception {
        when(userService.findByEmail("contribuyente@test.com"))
                .thenReturn(Optional.of(userConTaxPayer("contribuyente@test.com", "tp-001")));
        when(taxObligationService.findByTaxPayerId("tp-001"))
                .thenReturn(List.of());

        mockMvc.perform(get("/portal/obligaciones"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("obligaciones", hasSize(0)));
    }

    @Test
    @WithMockUser(username = "contribuyente@test.com", roles = "CONTRIBUYENTE")
    void obligaciones_renderizaTablaConEnlaceVerDetalle() throws Exception {
        when(userService.findByEmail("contribuyente@test.com"))
                .thenReturn(Optional.of(userConTaxPayer("contribuyente@test.com", "tp-001")));
        when(taxObligationService.findByTaxPayerId("tp-001"))
                .thenReturn(List.of(ejemploObligacion("ob-1")));

        mockMvc.perform(get("/portal/obligaciones"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ver detalle")));
    }

    @Test
    @WithMockUser(username = "contribuyente@test.com", roles = "CONTRIBUYENTE")
    void detalleObligacion_obligacionPropia_pueblaModel() throws Exception {
        when(userService.findByEmail("contribuyente@test.com"))
                .thenReturn(Optional.of(userConTaxPayer("contribuyente@test.com", "tp-001")));
        when(taxObligationService.findByTaxPayerId("tp-001"))
                .thenReturn(List.of(ejemploObligacion("ob-001")));
        when(documentService.findByObligation("ob-001"))
                .thenReturn(List.of(
                        new com.cronos.gestiontributaria.obligaciones.model.Document(),
                        new com.cronos.gestiontributaria.obligaciones.model.Document()));

        mockMvc.perform(get("/portal/obligaciones/ob-001"))
                .andExpect(status().isOk())
                .andExpect(view().name("portal/obligacion-detalle"))
                .andExpect(model().attributeExists("obligacion"))
                .andExpect(model().attribute("documentos", hasSize(2)));
    }

    // ─── D-4: detalle de obligación ─────────────────────────────────────

    @Test
    @WithMockUser(username = "contribuyente@test.com", roles = "CONTRIBUYENTE")
    void detalleObligacion_obligacionAjena_retorna403() throws Exception {
        when(userService.findByEmail("contribuyente@test.com"))
                .thenReturn(Optional.of(userConTaxPayer("contribuyente@test.com", "tp-001")));
        // El contribuyente no tiene ninguna obligación con id ob-999.
        when(taxObligationService.findByTaxPayerId("tp-001"))
                .thenReturn(List.of());

        mockMvc.perform(get("/portal/obligaciones/ob-999"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "contribuyente@test.com", roles = "CONTRIBUYENTE")
    void detalleObligacion_renderizaBotonSubirDocumento() throws Exception {
        when(userService.findByEmail("contribuyente@test.com"))
                .thenReturn(Optional.of(userConTaxPayer("contribuyente@test.com", "tp-001")));
        when(taxObligationService.findByTaxPayerId("tp-001"))
                .thenReturn(List.of(ejemploObligacion("ob-001")));
        when(documentService.findByObligation("ob-001"))
                .thenReturn(List.of());

        mockMvc.perform(get("/portal/obligaciones/ob-001"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Subir documento")));
    }

    @Test
    @WithMockUser(username = "contribuyente@test.com", roles = "CONTRIBUYENTE")
    void detalleObligacion_renderizaPeriodoFiscalDelRecord() throws Exception {
        // Valida que los accessors del record DTO funcionan en el template.
        when(userService.findByEmail("contribuyente@test.com"))
                .thenReturn(Optional.of(userConTaxPayer("contribuyente@test.com", "tp-001")));
        when(taxObligationService.findByTaxPayerId("tp-001"))
                .thenReturn(List.of(ejemploObligacion("ob-001")));
        when(documentService.findByObligation("ob-001"))
                .thenReturn(List.of());

        mockMvc.perform(get("/portal/obligaciones/ob-001"))
                .andExpect(status().isOk())
                // El periodo fiscal del ejemploObligacion es "2026".
                .andExpect(content().string(containsString("2026")));
    }

    // ─── D-5: fragmento de subida en /portal/documentos ─────────────────

    @Test
    @WithMockUser(username = "contribuyente@test.com", roles = "CONTRIBUYENTE")
    void documentos_renderizaSeccionDeSubida() throws Exception {
        mockMvc.perform(get("/portal/documentos"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Subir documento")));
    }
}
