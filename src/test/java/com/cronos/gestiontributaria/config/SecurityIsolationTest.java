package com.cronos.gestiontributaria.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import java.util.Optional;
import com.cronos.gestiontributaria.auth.model.User;
import com.cronos.gestiontributaria.auth.service.UserService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
class SecurityIsolationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        User mockUser = new User();
        mockUser.setEmail("admin@cronos.com");
        when(userService.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
    }

    @Test
    void unauthenticatedUserShouldBeRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/portal"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/login"));
               
        mockMvc.perform(get("/dashboard"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "contribuyente@prueba.com", roles = "CONTRIBUYENTE")
    void contribuyenteCannotAccessAdminRoutes() throws Exception {
        mockMvc.perform(get("/dashboard"))
               .andExpect(status().isForbidden());
               
        mockMvc.perform(get("/clientes"))
               .andExpect(status().isForbidden());
               
        mockMvc.perform(get("/obligaciones"))
               .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@cronos.com", roles = "GERENTE")
    void gerenteCannotAccessPortalRoutes() throws Exception {
        // Gerente shouldn't access the portal MVC
        mockMvc.perform(get("/portal/inicio"))
               .andExpect(status().isForbidden());
               
        // Gerente shouldn't access taxpayer REST APIs
        mockMvc.perform(get("/api/contribuyente/me"))
               .andExpect(status().isForbidden());
    }
    
    @Test
    @WithMockUser(username = "admin@cronos.com", roles = "GERENTE")
    void gerenteCanAccessAdminRoutes() throws Exception {
        mockMvc.perform(get("/dashboard"))
               .andExpect(status().isOk());
               
        mockMvc.perform(get("/clientes"))
               .andExpect(status().isOk());
    }
}
