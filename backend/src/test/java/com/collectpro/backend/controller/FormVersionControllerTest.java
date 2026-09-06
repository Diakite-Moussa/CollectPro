package com.collectpro.backend.controller;

import com.collectpro.backend.config.MethodSecurityConfig;
import com.collectpro.backend.dto.CreateFormVersionRequest;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FormVersionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityConfig.class)
class FormVersionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private FormVersionService formVersionService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean(name = "securityAuth")
    private SecurityAuthorizationService securityAuth;

    private User adminUser;
    private UsernamePasswordAuthenticationToken authPrincipal;

    @BeforeEach
    void setUp() {
        Organization organization = Organization.builder().id(1L).name("Org").build();
        Role adminRole = Role.builder().id(3L).name(RoleType.ADMIN_PRINCIPAL).build();

        adminUser = User.builder()
                .id(1L)
                .email("admin@test.com")
                .firstName("Admin")
                .lastName("Principal")
                .organization(organization)
                .role(adminRole)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(adminUser);
        authPrincipal = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authPrincipal);

        // Autorise systématiquement les vérifications @PreAuthorize("@securityAuth...")
        lenient().when(securityAuth.hasRole(any(), any())).thenReturn(true);
        lenient().when(securityAuth.hasAnyRole(any(), any(String[].class))).thenReturn(true);
        lenient().when(securityAuth.hasPermission(any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("POST /forms/{formId}/versions - Crée une version et retourne 201")
    void createVersion_ReturnsCreated() throws Exception {
        CreateFormVersionRequest request = new CreateFormVersionRequest();
        request.setSchemaJson("{\"fields\":[]}");

        FormVersionResponse response = FormVersionResponse.builder()
                .id(1L)
                .formId(5L)
                .versionNumber(1)
                .schemaJson("{\"fields\":[]}")
                .createdAt(LocalDateTime.now())
                .createdById(1L)
                .build();

        when(formVersionService.createVersion(eq(5L), any(), any())).thenReturn(response);

        mockMvc.perform(post("/forms/5/versions")
                        .principal(authPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.formId").value(5))
                .andExpect(jsonPath("$.versionNumber").value(1));
    }

    @Test
    @DisplayName("POST /forms/{formId}/versions - Refuse sans la permission CREATE_FORM")
    void createVersion_WithoutPermission_ReturnsForbidden() throws Exception {
        when(securityAuth.hasPermission(any(), eq("CREATE_FORM"))).thenReturn(false);

        CreateFormVersionRequest request = new CreateFormVersionRequest();
        request.setSchemaJson("{\"fields\":[]}");

        mockMvc.perform(post("/forms/5/versions")
                        .principal(authPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(formVersionService, never()).createVersion(any(), any(), any());
    }

    @Test
    @DisplayName("POST /forms/{formId}/versions - Refuse un schemaJson vide (validation)")
    void createVersion_BlankSchema_ReturnsBadRequest() throws Exception {
        CreateFormVersionRequest request = new CreateFormVersionRequest();
        request.setSchemaJson("");

        mockMvc.perform(post("/forms/5/versions")
                        .principal(authPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(formVersionService, never()).createVersion(any(), any(), any());
    }

    @Test
    @DisplayName("POST /forms/{formId}/versions - Formulaire introuvable (404)")
    void createVersion_FormNotFound_ReturnsNotFound() throws Exception {
        CreateFormVersionRequest request = new CreateFormVersionRequest();
        request.setSchemaJson("{\"fields\":[]}");

        when(formVersionService.createVersion(eq(999L), any(), any()))
                .thenThrow(new ResourceNotFoundException("Formulaire introuvable (id=999)"));

        mockMvc.perform(post("/forms/999/versions")
                        .principal(authPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Formulaire introuvable (id=999)"));
    }

    @Test
    @DisplayName("POST /forms/{formId}/versions - Refuse un formulaire d'une autre organisation (403 IDOR)")
    void createVersion_OtherOrganizationForm_ReturnsForbidden() throws Exception {
        CreateFormVersionRequest request = new CreateFormVersionRequest();
        request.setSchemaJson("{\"fields\":[]}");

        when(formVersionService.createVersion(eq(5L), any(), any()))
                .thenThrow(new ForbiddenOperationException("Ce formulaire n'appartient pas à votre organisation"));

        mockMvc.perform(post("/forms/5/versions")
                        .principal(authPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Ce formulaire n'appartient pas à votre organisation"));
    }

    @Test
    @DisplayName("GET /forms/{formId}/versions - Retourne la liste des versions triée")
    void getVersions_ReturnsList() throws Exception {
        FormVersionResponse v2 = FormVersionResponse.builder()
                .id(2L).formId(5L).versionNumber(2).schemaJson("{}").createdById(1L).build();
        FormVersionResponse v1 = FormVersionResponse.builder()
                .id(1L).formId(5L).versionNumber(1).schemaJson("{}").createdById(1L).build();

        when(formVersionService.getVersionsForForm(eq(5L), any())).thenReturn(List.of(v2, v1));

        mockMvc.perform(get("/forms/5/versions").principal(authPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].versionNumber").value(2))
                .andExpect(jsonPath("$[1].versionNumber").value(1));
    }

    @Test
    @DisplayName("GET /forms/{formId}/versions - Formulaire introuvable (404)")
    void getVersions_FormNotFound_ReturnsNotFound() throws Exception {
        when(formVersionService.getVersionsForForm(eq(999L), any()))
                .thenThrow(new ResourceNotFoundException("Formulaire introuvable (id=999)"));

        mockMvc.perform(get("/forms/999/versions").principal(authPrincipal))
                .andExpect(status().isNotFound());
    }
}