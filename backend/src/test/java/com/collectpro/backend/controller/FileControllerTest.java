package com.collectpro.backend.controller;

import com.collectpro.backend.config.MethodSecurityConfig;
import com.collectpro.backend.entity.CollecteAttachment;
import com.collectpro.backend.entity.Organization;
import com.collectpro.backend.entity.Role;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.exception.ResourceNotFoundException;
import com.collectpro.backend.repository.CollecteAttachmentRepository;
import com.collectpro.backend.security.CustomUserDetails;
import com.collectpro.backend.security.CustomUserDetailsService;
import com.collectpro.backend.security.JwtService;
import com.collectpro.backend.security.SecurityAuthorizationService;
import com.collectpro.backend.service.CollecteService;
import com.collectpro.backend.service.OrganizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FileController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityConfig.class)
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CollecteService collecteService;

    @MockitoBean
    private CollecteAttachmentRepository attachmentRepository;

    @MockitoBean
    private OrganizationService organizationService;

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
        Organization org = Organization.builder().id(1L).name("Org").build();
        Role agentRole = Role.builder().id(4L).name(RoleType.AGENT).build();

        agentUser = User.builder()
                .id(15L)
                .email("agent@test.com")
                .organization(org)
                .role(agentRole)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(agentUser);
        authPrincipal = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authPrincipal);
    }

    @Test
    @DisplayName("GET /files/attachments/{id} - Télécharge la pièce jointe pour un utilisateur authentifié autorisé")
    void downloadAttachment_Authorized_ReturnsFile() throws Exception {
        CollecteAttachment attachment = CollecteAttachment.builder()
                .id(50L)
                .originalFilename("photo.jpg")
                .storedFilename("uuid-stored.jpg")
                .contentType("image/jpeg")
                .sizeBytes(1024L)
                .build();

        Resource resource = new ByteArrayResource("fake-image-bytes".getBytes());

        when(attachmentRepository.findById(50L)).thenReturn(Optional.of(attachment));
        when(collecteService.loadAttachmentResource(eq(50L), any())).thenReturn(resource);

        mockMvc.perform(get("/files/attachments/50").principal(authPrincipal))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"photo.jpg\""))
                .andExpect(content().bytes("fake-image-bytes".getBytes()));
    }

    @Test
    @DisplayName("GET /files/attachments/{id} - 404 si la pièce jointe n'existe pas")
    void downloadAttachment_NotFound_Returns404() throws Exception {
        when(attachmentRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/files/attachments/99").principal(authPrincipal))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /files/attachments/{id} - 404 si l'utilisateur n'a pas accès à cette collecte")
    void downloadAttachment_Forbidden_Returns404() throws Exception {
        CollecteAttachment attachment = CollecteAttachment.builder()
                .id(50L)
                .originalFilename("photo.jpg")
                .storedFilename("uuid-stored.jpg")
                .contentType("image/jpeg")
                .sizeBytes(1024L)
                .build();

        when(attachmentRepository.findById(50L)).thenReturn(Optional.of(attachment));
        when(collecteService.loadAttachmentResource(eq(50L), any()))
                .thenThrow(new ResourceNotFoundException("Fichier introuvable"));

        mockMvc.perform(get("/files/attachments/50").principal(authPrincipal))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /files/organizations/{id}/logo - Retourne le logo (endpoint public, sans principal)")
    void downloadOrganizationLogo_ReturnsLogo() throws Exception {
        Resource resource = new ByteArrayResource("fake-png-bytes".getBytes());
        OrganizationService.LogoFile logoFile = new OrganizationService.LogoFile(resource, "image/png");

        when(organizationService.loadLogoFile(1L)).thenReturn(logoFile);

        mockMvc.perform(get("/files/organizations/1/logo"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(content().bytes("fake-png-bytes".getBytes()));
    }

    @Test
    @DisplayName("GET /files/organizations/{id}/logo - 404 si aucun logo défini")
    void downloadOrganizationLogo_NoLogo_Returns404() throws Exception {
        when(organizationService.loadLogoFile(1L))
                .thenThrow(new ResourceNotFoundException("Logo non défini pour cette organisation"));

        mockMvc.perform(get("/files/organizations/1/logo"))
                .andExpect(status().isNotFound());
    }
}