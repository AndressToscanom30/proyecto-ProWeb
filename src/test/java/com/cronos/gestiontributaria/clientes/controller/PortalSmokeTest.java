package com.cronos.gestiontributaria.clientes.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.cronos.gestiontributaria.auth.model.Role;
import com.cronos.gestiontributaria.auth.model.User;
import com.cronos.gestiontributaria.auth.service.UserService;
import com.cronos.gestiontributaria.clientes.model.TaxPayer;
import com.cronos.gestiontributaria.clientes.service.TaxPayerService;
import com.cronos.gestiontributaria.obligaciones.service.DocumentService;
import com.cronos.gestiontributaria.obligaciones.service.TaxObligationService;

@SpringBootTest
@ActiveProfiles("test")
class PortalSmokeTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private TaxPayerService taxPayerService;

    @MockitoBean
    private TaxObligationService taxObligationService;

    @MockitoBean
    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        // Setup a mock user and taxpayer for the portal requests
        User mockUser = new User();
        mockUser.setEmail("contribuyente@prueba.com");
        mockUser.setRole(Role.contribuyente());
        mockUser.setTaxPayerId("test-taxpayer-id");
        
        TaxPayer mockTaxPayer = new TaxPayer();
        mockTaxPayer.setId("test-taxpayer-id");
        mockTaxPayer.setBusinessName("Contribuyente de Prueba (Smoke Test)");
        mockTaxPayer.setIdentificacion("900123456-2");
        
        when(userService.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(taxPayerService.findById("test-taxpayer-id")).thenReturn(mockTaxPayer);
        when(taxObligationService.findByTaxPayerId("test-taxpayer-id")).thenReturn(Collections.emptyList());
        when(documentService.findByTaxPayer("test-taxpayer-id")).thenReturn(Collections.emptyList());
    }

    @Test
    @WithMockUser(username = "contribuyente@prueba.com", roles = "CONTRIBUYENTE")
    void shouldRenderInicio() throws Exception {
        mockMvc.perform(get("/portal/inicio"))
               .andExpect(status().isOk())
               .andExpect(view().name("portal/inicio"))
               .andExpect(content().string(org.hamcrest.Matchers.containsString("Contribuyente de Prueba (Smoke Test)")));
    }

    @Test
    @WithMockUser(username = "contribuyente@prueba.com", roles = "CONTRIBUYENTE")
    void shouldRenderObligaciones() throws Exception {
        mockMvc.perform(get("/portal/obligaciones"))
               .andExpect(status().isOk())
               .andExpect(view().name("portal/obligaciones"))
               .andExpect(content().string(org.hamcrest.Matchers.containsString("Mis obligaciones tributarias")));
    }

    @Test
    @WithMockUser(username = "contribuyente@prueba.com", roles = "CONTRIBUYENTE")
    void shouldRenderDocumentos() throws Exception {
        mockMvc.perform(get("/portal/documentos"))
               .andExpect(status().isOk())
               .andExpect(view().name("portal/documentos"))
               .andExpect(content().string(org.hamcrest.Matchers.containsString("Mis documentos")));
    }
}
