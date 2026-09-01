package com.collectpro.backend.service;

import com.collectpro.backend.dto.CreateOrganizationRequest;
import com.collectpro.backend.dto.OrganizationResponse;
import com.collectpro.backend.entity.Organization;
import com.collectpro.backend.entity.Role;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.AuditAction;
import com.collectpro.backend.enums.OrganizationStatus;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.enums.UserStatus;
import com.collectpro.backend.repository.OrganizationRepository;
import com.collectpro.backend.repository.RoleRepository;
import com.collectpro.backend.repository.UserRepository;
import com.collectpro.backend.security.TokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private TokenUtil tokenUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserInvitationService userInvitationService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private OrganizationService organizationService;

    private User superAdmin;
    private Role adminPrincipalRole;

    @BeforeEach
    void setUp() {
        Role superAdminRole = Role.builder().id(1L).name(RoleType.SUPER_ADMIN).build();
        adminPrincipalRole = Role.builder().id(2L).name(RoleType.ADMIN_PRINCIPAL).build();

        superAdmin = User.builder()
                .id(1L)
                .firstName("Super")
                .lastName("Admin")
                .email("superadmin@collectpro.com")
                .role(superAdminRole)
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("createOrganizationWithPrincipalAdmin - Succès avec log d'audit")
    void createOrganization_Success() {
        CreateOrganizationRequest request = new CreateOrganizationRequest();
        request.setName("ONG Santé Sénégal");
        request.setDescription("Organisation partenaire santé");
        request.setAdminFirstName("Moussa");
        request.setAdminLastName("Diakitée");
        request.setAdminEmail("moussa.admin@ongsante.sn");
        request.setAdminPhone("+221771112233");

        when(userRepository.existsByEmail("moussa.admin@ongsante.sn")).thenReturn(false);
        when(organizationRepository.save(any(Organization.class))).thenAnswer(inv -> {
            Organization org = inv.getArgument(0);
            org.setId(100L);
            return org;
        });
        when(roleRepository.findByName(RoleType.ADMIN_PRINCIPAL)).thenReturn(Optional.of(adminPrincipalRole));
        when(tokenUtil.generateRawToken()).thenReturn("raw-token");
        when(passwordEncoder.encode("raw-token")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(200L);
            return u;
        });

        OrganizationResponse response = organizationService.createOrganizationWithPrincipalAdmin(superAdmin, request);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("ONG Santé Sénégal", response.getName());
        verify(userInvitationService).sendInvitation(any(User.class));
        verify(auditLogService).logAfterCommit(
                eq(superAdmin),
                any(Organization.class),
                eq(AuditAction.ORGANIZATION_CREATED),
                eq("Organization"),
                eq(100L),
                anyString()
        );
    }
}
