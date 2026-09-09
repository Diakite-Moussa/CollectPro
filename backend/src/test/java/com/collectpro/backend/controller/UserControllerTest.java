package com.collectpro.backend.controller;

import com.collectpro.backend.dto.AssignSupervisorRequest;
import com.collectpro.backend.dto.ChangePasswordRequest;
import com.collectpro.backend.dto.CreateUserRequest;
import com.collectpro.backend.dto.UpdateProfileRequest;
import com.collectpro.backend.dto.UserResponse;
import com.collectpro.backend.entity.Organization;
import com.collectpro.backend.entity.Role;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.security.CustomUserDetails;
import com.collectpro.backend.security.CustomUserDetailsService;
import com.collectpro.backend.security.JwtService;
import com.collectpro.backend.security.SecurityAuthorizationService;
import com.collectpro.backend.service.PermissionService;
import com.collectpro.backend.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import com.collectpro.backend.config.MethodSecurityConfig;
import org.springframework.context.annotation.Import;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private PermissionService permissionService;

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
        Organization org = Organization.builder().id(1L).name("Org").build();
        Role adminRole = Role.builder().id(3L).name(RoleType.ADMIN_PRINCIPAL).build();

        adminUser = User.builder()
                .id(1L)
                .email("admin@test.com")
                .firstName("Admin")
                .lastName("Principal")
                .organization(org)
                .role(adminRole)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(adminUser);
        authPrincipal = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authPrincipal);

        // Autoriser systématiquement les vérifications @PreAuthorize("@securityAuth...") pour les tests controller
        lenient().when(securityAuth.hasRole(any(), any())).thenReturn(true);
        lenient().when(securityAuth.hasAnyRole(any(), any(String[].class))).thenReturn(true);
        lenient().when(securityAuth.hasPermission(any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("GET /users - Retourne la liste des utilisateurs")
    void getUsers_ReturnsList() throws Exception {
        UserResponse uResp = UserResponse.builder()
                .id(10L)
                .email("agent@test.com")
                .firstName("Agent")
                .lastName("Un")
                .build();

        when(userService.getUsers(any(), any())).thenReturn(new PageImpl<>(List.of(uResp)));

        mockMvc.perform(get("/users").principal(authPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].email").value("agent@test.com"));
    }

    @Test
    @DisplayName("POST /users/assign-supervisor - Affecte un superviseur à un agent")
    void assignSupervisor_ReturnsNoContent() throws Exception {
        AssignSupervisorRequest req = new AssignSupervisorRequest();
        req.setAgentId(10L);
        req.setSupervisorId(20L);

        doNothing().when(userService).assignAgentToSupervisor(any(), any());

        mockMvc.perform(post("/users/assign-supervisor")
                        .principal(authPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /users/{id}/resend-invitation - Renvoyer invitation")
    void resendInvitation_ReturnsNoContent() throws Exception {
        doNothing().when(userService).resendInvitation(any(), any());

        mockMvc.perform(post("/users/10/resend-invitation").principal(authPrincipal))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /users - Refuse un rôle sans la permission CREATE_USER")
    void createUser_WithoutPermission_ReturnsForbidden() throws Exception {
        when(securityAuth.hasPermission(any(), eq("CREATE_USER"))).thenReturn(false);

        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("nouvel.agent@test.com");
        request.setFirstName("Nouvel");
        request.setLastName("Agent");
        request.setRoleType(RoleType.AGENT);

        mockMvc.perform(post("/users")
                        .principal(authPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(userService, never()).createUser(any(), any());
    }

    @Test
    @DisplayName("GET /users/{id}/permissions - Refuse un Superviseur (ni ADMIN_PRINCIPAL ni SUPER_ADMIN)")
    void getUserPermissions_WrongRole_ReturnsForbidden() throws Exception {
        when(securityAuth.hasAnyRole(any(), eq("ADMIN_PRINCIPAL"), eq("SUPER_ADMIN"))).thenReturn(false);

        mockMvc.perform(get("/users/10/permissions").principal(authPrincipal))
                .andExpect(status().isForbidden());

        verify(permissionService, never()).getUserPermissions(any(), any());
    }

    @Test
    @DisplayName("GET /users/me - Retourne le profil de l'utilisateur connecté")
    void getMe_ReturnsCurrentUser() throws Exception {
        UserResponse meResp = UserResponse.builder()
                .id(1L)
                .email("admin@test.com")
                .firstName("Admin")
                .lastName("Principal")
                .build();

        when(userService.getCurrentUser(any())).thenReturn(meResp);

        mockMvc.perform(get("/users/me").principal(authPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@test.com"))
                .andExpect(jsonPath("$.firstName").value("Admin"));
    }

    @Test
    @DisplayName("PUT /users/me - Met à jour le profil et retourne le nouveau UserResponse")
    void updateProfile_ReturnsUpdatedUser() throws Exception {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFirstName("Nouveau");
        req.setLastName("Nom");
        req.setPhone("+221771234567");

        UserResponse updated = UserResponse.builder()
                .id(1L)
                .firstName("Nouveau")
                .lastName("Nom")
                .email("admin@test.com")
                .build();

        when(userService.updateProfile(any(), any())).thenReturn(updated);

        mockMvc.perform(put("/users/me")
                        .principal(authPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Nouveau"))
                .andExpect(jsonPath("$.lastName").value("Nom"));
    }

    @Test
    @DisplayName("PUT /users/me/password - Retourne 204 No Content après changement")
    void changePassword_ReturnsNoContent() throws Exception {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("OldPass123");
        req.setNewPassword("NewPass456");

        doNothing().when(userService).changePassword(any(), any());

        mockMvc.perform(put("/users/me/password")
                        .principal(authPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());
    }
}
