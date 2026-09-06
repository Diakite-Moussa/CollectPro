package com.collectpro.backend.controller;

import com.collectpro.backend.config.MethodSecurityConfig;
import com.collectpro.backend.dto.CreateFormRequest;
import com.collectpro.backend.dto.FormResponse;
import com.collectpro.backend.entity.Organization;
import com.collectpro.backend.entity.Role;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.FormStatus;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.exception.BusinessRuleException;
import com.collectpro.backend.security.CustomUserDetails;
import com.collectpro.backend.security.CustomUserDetailsService;
import com.collectpro.backend.security.JwtService;
import com.collectpro.backend.security.SecurityAuthorizationService;
import com.collectpro.backend.service.FormService;
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

@WebMvcTest(FormController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityConfig.class)
class FormControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private FormService formService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean(name = "securityAuth")
    private SecurityAuthorizationService securityAuth;

    private User adminUser;
    private Organization organization;
    private UsernamePasswordAuthenticationToken authPrincipal;

    @BeforeEach
    void setUp() {
        organization = Organization.builder().id(1L).name("Org").build();
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
    @DisplayName("POST /forms - Crée un formulaire et retourne 201")
    void createForm_ReturnsCreated() throws Exception {
        CreateFormRequest request = new CreateFormRequest();
        request.setName("Formulaire enquête");
        request.setDescription("Description");

        FormResponse response = FormResponse.builder()
                .id(1L)
                .name("Formulaire enquête")
                .description("Description")
                .status(FormStatus.DRAFT)
                .createdAt(LocalDateTime.now())
                .organizationId(1L)
                .build();

        when(formService.createForm(any(), any())).thenReturn(response);

        mockMvc.perform(post("/forms")
                        .principal(authPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Formulaire enquête"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @DisplayName("POST /forms - Refuse sans la permission CREATE_FORM")
    void createForm_WithoutPermission_ReturnsForbidden() throws Exception {
        when(securityAuth.hasPermission(any(), eq("CREATE_FORM"))).thenReturn(false);

        CreateFormRequest request = new CreateFormRequest();
        request.setName("Formulaire enquête");

        mockMvc.perform(post("/forms")
                        .principal(authPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(formService, never()).createForm(any(), any());
    }

    @Test
    @DisplayName("POST /forms - Refuse un nom vide (validation)")
    void createForm_BlankName_ReturnsBadRequest() throws Exception {
        CreateFormRequest request = new CreateFormRequest();
        request.setName("");

        mockMvc.perform(post("/forms")
                        .principal(authPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(formService, never()).createForm(any(), any());
    }

    @Test
    @DisplayName("GET /forms - Retourne la liste des formulaires de l'organisation")
    void getForms_ReturnsList() throws Exception {
        FormResponse response = FormResponse.builder()
                .id(1L)
                .name("Formulaire A")
                .status(FormStatus.DRAFT)
                .organizationId(1L)
                .build();

        when(formService.getFormsForOrganization(any())).thenReturn(List.of(response));

        mockMvc.perform(get("/forms").principal(authPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Formulaire A"));
    }

    @Test
    @DisplayName("GET /forms/published - Retourne uniquement les formulaires publiés")
    void getPublishedForms_ReturnsList() throws Exception {
        FormResponse response = FormResponse.builder()
                .id(2L)
                .name("Formulaire publié")
                .status(FormStatus.PUBLISHED)
                .organizationId(1L)
                .build();

        when(formService.getPublishedFormsForOrganization(any())).thenReturn(List.of(response));

        mockMvc.perform(get("/forms/published").principal(authPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("POST /forms/{id}/publish - Publie le formulaire")
    void publishForm_ReturnsUpdatedForm() throws Exception {
        FormResponse response = FormResponse.builder()
                .id(1L)
                .name("Formulaire A")
                .status(FormStatus.PUBLISHED)
                .organizationId(1L)
                .build();

        when(formService.publishForm(any(), eq(1L))).thenReturn(response);

        mockMvc.perform(post("/forms/1/publish").principal(authPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("POST /forms/{id}/publish - Refuse sans la permission PUBLISH_FORM")
    void publishForm_WithoutPermission_ReturnsForbidden() throws Exception {
        when(securityAuth.hasPermission(any(), eq("PUBLISH_FORM"))).thenReturn(false);

        mockMvc.perform(post("/forms/1/publish").principal(authPrincipal))
                .andExpect(status().isForbidden());

        verify(formService, never()).publishForm(any(), any());
    }

    @Test
    @DisplayName("POST /forms/{id}/publish - Erreur métier (409) si formulaire archivé ou sans version")
    void publishForm_BusinessRuleViolation_ReturnsConflict() throws Exception {
        when(formService.publishForm(any(), eq(1L)))
                .thenThrow(new BusinessRuleException("Impossible de publier un formulaire sans version"));

        mockMvc.perform(post("/forms/1/publish").principal(authPrincipal))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Impossible de publier un formulaire sans version"));
    }

    @Test
    @DisplayName("POST /forms/{id}/archive - Archive le formulaire")
    void archiveForm_ReturnsUpdatedForm() throws Exception {
        FormResponse response = FormResponse.builder()
                .id(1L)
                .name("Formulaire A")
                .status(FormStatus.ARCHIVED)
                .organizationId(1L)
                .build();

        when(formService.archiveForm(any(), eq(1L))).thenReturn(response);

        mockMvc.perform(post("/forms/1/archive").principal(authPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    @Test
    @DisplayName("POST /forms/{id}/archive - Refuse sans la permission PUBLISH_FORM")
    void archiveForm_WithoutPermission_ReturnsForbidden() throws Exception {
        when(securityAuth.hasPermission(any(), eq("PUBLISH_FORM"))).thenReturn(false);

        mockMvc.perform(post("/forms/1/archive").principal(authPrincipal))
                .andExpect(status().isForbidden());

        verify(formService, never()).archiveForm(any(), any());
    }
}