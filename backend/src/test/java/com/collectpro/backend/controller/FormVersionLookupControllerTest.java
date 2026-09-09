package com.collectpro.backend.controller;

import com.collectpro.backend.config.MethodSecurityConfig;
import com.collectpro.backend.dto.FormVersionResponse;
import com.collectpro.backend.entity.Organization;
import com.collectpro.backend.entity.Role;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.exception.ForbiddenOperationException;
import com.collectpro.backend.exception.ResourceNotFoundException;
import com.collectpro.backend.security.CustomUserDetails;
import com.collectpro.backend.security.CustomUserDetailsService;
import com.collectpro.backend.security.JwtService;
import com.collectpro.backend.security.SecurityAuthorizationService;
import com.collectpro.backend.service.FormVersionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FormVersionLookupController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityConfig.class)
class FormVersionLookupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FormVersionService formVersionService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean(name = "securityAuth")
    private SecurityAuthorizationService securityAuth;

    private User agentUser;
    private UsernamePasswordAuthenticationToken authPrincipal;

    @BeforeEach
    void setUp() {
        Organization organization = Organization.builder().id(1L).name("Org").build();
        Role agentRole = Role.builder().id(5L).name(RoleType.AGENT).build();

        agentUser = User.builder()
                .id(1L)
                .email("agent@test.com")
                .firstName("Agent")
                .lastName("Terrain")
                .organization(organization)
                .role(agentRole)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(agentUser);
        authPrincipal = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authPrincipal);

        lenient().when(securityAuth.hasRole(any(), any())).thenReturn(true);
        lenient().when(securityAuth.hasAnyRole(any(), any(String[].class))).thenReturn(true);
        lenient().when(securityAuth.hasPermission(any(), any())).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /form-versions/{id} - Retourne la version demandée")
    void getVersion_ReturnsOk() throws Exception {
        FormVersionResponse response = FormVersionResponse.builder()
                .id(1L)
                .formId(5L)
                .versionNumber(2)
                .schemaJson("{\"fields\":[]}")
                .createdAt(LocalDateTime.now())
                .createdById(1L)
                .build();

        when(formVersionService.getVersionResponse(eq(1L), any())).thenReturn(response);

        mockMvc.perform(get("/form-versions/1").principal(authPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.formId").value(5))
                .andExpect(jsonPath("$.versionNumber").value(2));
    }

    @Test
    @DisplayName("GET /form-versions/{id} - Version introuvable (404)")
    void getVersion_NotFound_ReturnsNotFound() throws Exception {
        when(formVersionService.getVersionResponse(eq(999L), any()))
                .thenThrow(new ResourceNotFoundException("Version de formulaire introuvable (id=999)"));

        mockMvc.perform(get("/form-versions/999").principal(authPrincipal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Version de formulaire introuvable (id=999)"));
    }

    @Test
    @DisplayName("GET /form-versions/{id} - Refuse une version d'une autre organisation (403 IDOR)")
    void getVersion_OtherOrganization_ReturnsForbidden() throws Exception {
        when(formVersionService.getVersionResponse(eq(1L), any()))
                .thenThrow(new ForbiddenOperationException("Cette version ne vous est pas accessible"));

        mockMvc.perform(get("/form-versions/1").principal(authPrincipal))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Cette version ne vous est pas accessible"));
    }

    @Test
    @DisplayName("GET /form-versions/{id} - Refuse sans authentification (401/403)")
    void getVersion_Unauthenticated_ReturnsError() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/form-versions/1"))
                .andExpect(status().is4xxClientError());
    }
}