package com.cronos.gestiontributaria.clientes.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.cronos.gestiontributaria.common.TaxpayerType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;
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
@Import(TaxPayerRestControllerTest.MethodSecurityTestConfig.class)
class TaxPayerRestControllerTest {

    /**
     * Habilita {@code @PreAuthorize} en el contexto de test sin necesidad de
     * importar el {@code SecurityConfig} real (que arrastra dependencias de
     * {@code DaoAuthenticationProvider} y {@code CustomUserDetailsService}).
     */
    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }


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

    // ─── C-2: POST /me/documentos ────────────────────────────────────────

    @Test
    @WithMockUser(username = "contribuyente@test.com", roles = "CONTRIBUYENTE")
    void subirDocumento_archivoValido_retorna201() throws Exception {
        when(userService.findByEmail("contribuyente@test.com"))
                .thenReturn(Optional.of(userConTaxPayer("contribuyente@test.com", "tp-001")));
        Document doc = new Document();
        doc.setId("doc-001");
        doc.setFileName("contrato.pdf");
        when(documentService.uploadDocument(eq("tp-001"), isNull(), any(), isNull()))
                .thenReturn(doc);

        MockMultipartFile file = new MockMultipartFile(
                "file", "contrato.pdf", "application/pdf", new byte[1024]);

        mockMvc.perform(multipart("/api/contribuyente/me/documentos")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("doc-001"));
    }

    @Test
    @WithMockUser(username = "contribuyente@test.com", roles = "CONTRIBUYENTE")
    void subirDocumento_conObligacionPropia_retorna201() throws Exception {
        when(userService.findByEmail("contribuyente@test.com"))
                .thenReturn(Optional.of(userConTaxPayer("contribuyente@test.com", "tp-001")));
        // La obligación ob-001 pertenece a tp-001.
        TaxObligationResponseDTO dto = new TaxObligationResponseDTO(
                "ob-001", "tp-001", "Empresa", "900", null, null, "2026-01", 2026,
                null, false, null, null, null);
        when(taxObligationService.findByTaxPayerId("tp-001"))
                .thenReturn(List.of(dto));

        Document doc = new Document();
        doc.setId("doc-002");
        when(documentService.uploadDocument(eq("tp-001"), eq("ob-001"), any(), isNull()))
                .thenReturn(doc);

        MockMultipartFile file = new MockMultipartFile(
                "file", "factura.pdf", "application/pdf", new byte[1024]);

        mockMvc.perform(multipart("/api/contribuyente/me/documentos")
                        .file(file)
                        .param("obligationId", "ob-001")
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("doc-002"));
    }

    @Test
    @WithMockUser(username = "contribuyente@test.com", roles = "CONTRIBUYENTE")
    void subirDocumento_conObligacionAjena_retorna403() throws Exception {
        when(userService.findByEmail("contribuyente@test.com"))
                .thenReturn(Optional.of(userConTaxPayer("contribuyente@test.com", "tp-001")));
        // Lista vacía: ninguna obligación con id ob-999 pertenece a tp-001.
        when(taxObligationService.findByTaxPayerId("tp-001"))
                .thenReturn(List.of());

        MockMultipartFile file = new MockMultipartFile(
                "file", "factura.pdf", "application/pdf", new byte[1024]);

        mockMvc.perform(multipart("/api/contribuyente/me/documentos")
                        .file(file)
                        .param("obligationId", "ob-999")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        // Importante: no debe invocarse uploadDocument.
        verify(documentService, never())
                .uploadDocument(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "contribuyente@test.com", roles = "CONTRIBUYENTE")
    void subirDocumento_tipoNoPermitido_retorna400() throws Exception {
        when(userService.findByEmail("contribuyente@test.com"))
                .thenReturn(Optional.of(userConTaxPayer("contribuyente@test.com", "tp-001")));
        when(documentService.uploadDocument(eq("tp-001"), isNull(), any(), isNull()))
                .thenThrow(new IllegalArgumentException(
                        "Tipo de archivo no permitido: video/mp4"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "video.mp4", "video/mp4", new byte[1024]);

        mockMvc.perform(multipart("/api/contribuyente/me/documentos")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void subirDocumento_sinAutenticacion_retorna401o403() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "x.pdf", "application/pdf", new byte[8]);

        MvcResult result = mockMvc.perform(multipart("/api/contribuyente/me/documentos")
                        .file(file)
                        .with(csrf()))
                .andReturn();
        int status = result.getResponse().getStatus();
        assertTrue(status == 401 || status == 403 || status == 302,
                "Sin autenticación se esperaba 401/403/302, fue: " + status);
    }

    // ─── C-3: PATCH /{id}/toggle-active ──────────────────────────────────

    @Test
    @WithMockUser(username = "gerente@test.com", roles = "GERENTE")
    void toggleActive_comoGerente_retorna200() throws Exception {
        TaxPayer tp = new TaxPayer();
        tp.setId("tp-001");
        tp.setActive(false);
        when(taxPayerService.toggleActive("tp-001")).thenReturn(tp);

        mockMvc.perform(patch("/api/contribuyente/tp-001/toggle-active")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("tp-001"))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @WithMockUser(username = "gerente@test.com", roles = "GERENTE")
    void toggleActive_idInexistente_retorna404() throws Exception {
        when(taxPayerService.toggleActive("tp-missing"))
                .thenThrow(new java.util.NoSuchElementException(
                        "Contribuyente no encontrado con ID: tp-missing"));

        mockMvc.perform(patch("/api/contribuyente/tp-missing/toggle-active")
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "contribuyente@test.com", roles = "CONTRIBUYENTE")
    void toggleActive_comoContribuyente_retorna403() throws Exception {
        mockMvc.perform(patch("/api/contribuyente/tp-001/toggle-active")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(taxPayerService, never()).toggleActive(any());
    }

    @Test
    void toggleActive_sinAutenticacion_retorna401o403() throws Exception {
        MvcResult result = mockMvc.perform(
                        patch("/api/contribuyente/tp-001/toggle-active").with(csrf()))
                .andReturn();
        int status = result.getResponse().getStatus();
        assertTrue(status == 401 || status == 403 || status == 302,
                "Sin autenticación se esperaba 401/403/302, fue: " + status);
    }

    // ─── C-4: GET /api/contribuyente (listado paginado) ──────────────────

    @Test
    @WithMockUser(username = "gerente@test.com", roles = "GERENTE")
    void listar_comoGerente_retorna200() throws Exception {
        TaxPayer tp1 = new TaxPayer();
        tp1.setId("tp-001");
        TaxPayer tp2 = new TaxPayer();
        tp2.setId("tp-002");
        Page<TaxPayer> pagina = new PageImpl<>(List.of(tp1, tp2),
                PageRequest.of(0, 20), 2);
        when(taxPayerService.findByFilters(any(), any(), any(), any()))
                .thenReturn(pagina);

        mockMvc.perform(get("/api/contribuyente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @WithMockUser(username = "asesor@test.com", roles = "ASESOR")
    void listar_comoAsesor_retorna200() throws Exception {
        when(taxPayerService.findByFilters(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/contribuyente"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "contribuyente@test.com", roles = "CONTRIBUYENTE")
    void listar_comoContribuyente_retorna403() throws Exception {
        mockMvc.perform(get("/api/contribuyente"))
                .andExpect(status().isForbidden());

        verify(taxPayerService, never()).findByFilters(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "gerente@test.com", roles = "GERENTE")
    void listar_conParametros_qYActivo_pasaAlServicio() throws Exception {
        when(taxPayerService.findByFilters(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/contribuyente")
                        .param("q", "empresa")
                        .param("activo", "true"))
                .andExpect(status().isOk());

        verify(taxPayerService).findByFilters(
                eq("empresa"), isNull(), eq(Boolean.TRUE), any());
    }
}
