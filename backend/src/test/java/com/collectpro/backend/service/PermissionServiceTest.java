package com.collectpro.backend.service;

import com.collectpro.backend.dto.PermissionResponse;
import com.collectpro.backend.dto.UpdateUserPermissionsRequest;
import com.collectpro.backend.dto.UserPermissionsResponse;
import com.collectpro.backend.entity.Organization;
import com.collectpro.backend.entity.Permission;
import com.collectpro.backend.entity.Role;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.entity.UserPermission;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.exception.BusinessRuleException;
import com.collectpro.backend.exception.ForbiddenOperationException;
import com.collectpro.backend.exception.ResourceNotFoundException;
import com.collectpro.backend.repository.PermissionRepository;
import com.collectpro.backend.repository.UserPermissionRepository;
import com.collectpro.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private UserPermissionRepository userPermissionRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PermissionService permissionService;

    private Organization orgA;
    private Organization orgB;
    private User superAdmin;
    private User adminPrincipalOrgA;
    private User adminSecondaireOrgA;
    private User adminSecondaireOrgB;
    private User agentOrgA;
    private Permission createFormPermission;

    @BeforeEach
    void setUp() {
        orgA = Organization.builder().id(1L).name("Org A").build();
        orgB = Organization.builder().id(2L).name("Org B").build();

        Role superAdminRole = Role.builder().id(1L).name(RoleType.SUPER_ADMIN).permissions(Set.of()).build();
        Role adminPrincipalRole = Role.builder().id(2L).name(RoleType.ADMIN_PRINCIPAL).permissions(Set.of()).build();
        Role adminSecondaireRole = Role.builder().id(3L).name(RoleType.ADMIN_SECONDAIRE).permissions(Set.of()).build();
        Role agentRole = Role.builder().id(4L).name(RoleType.AGENT).permissions(Set.of()).build();

        superAdmin = User.builder().id(1L).firstName("Super").lastName("Admin").role(superAdminRole).build();
        adminPrincipalOrgA = User.builder().id(2L).firstName("Admin").lastName("Principal")
                .organization(orgA).role(adminPrincipalRole).build();
        adminSecondaireOrgA = User.builder().id(3L).firstName("Admin").lastName("Secondaire A")
                .organization(orgA).role(adminSecondaireRole).build();
        adminSecondaireOrgB = User.builder().id(4L).firstName("Admin").lastName("Secondaire B")
                .organization(orgB).role(adminSecondaireRole).build();
        agentOrgA = User.builder().id(5L).firstName("Agent").lastName("Un")
                .organization(orgA).role(agentRole).build();

        createFormPermission = Permission.builder().id(10L).code("CREATE_FORM").description("Créer des formulaires").build();
    }

    @Test
    @DisplayName("getDelegatablePermissions() - Succès pour un Admin principal")
    void getDelegatablePermissions_ByAdminPrincipal_ReturnsList() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminPrincipalOrgA));

        List<PermissionResponse> result = permissionService.getDelegatablePermissions(adminPrincipalOrgA);

        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(p -> p.getCode().equals("CREATE_FORM")));
    }

    @Test
    @DisplayName("getDelegatablePermissions() - Échec pour un Agent (rôle non autorisé)")
    void getDelegatablePermissions_ByAgent_ThrowsForbidden() {
        when(userRepository.findById(5L)).thenReturn(Optional.of(agentOrgA));

        assertThrows(ForbiddenOperationException.class, () ->
                permissionService.getDelegatablePermissions(agentOrgA)
        );
    }

    @Test
    @DisplayName("updateUserPermissions() - Échec : permission non déléguable")
    void updateUserPermissions_NonDelegatableCode_ThrowsBusinessRuleException() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminPrincipalOrgA));
        when(userRepository.findById(3L)).thenReturn(Optional.of(adminSecondaireOrgA));

        UpdateUserPermissionsRequest request = new UpdateUserPermissionsRequest();
        request.setPermissionCodes(List.of("CREATE_ORGANIZATION")); // réservée au Super Admin, jamais déléguable

        assertThrows(BusinessRuleException.class, () ->
                permissionService.updateUserPermissions(adminPrincipalOrgA, 3L, request)
        );
        verify(userPermissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateUserPermissions() - Échec : cible n'est pas un Admin secondaire")
    void updateUserPermissions_TargetNotAdminSecondaire_ThrowsBusinessRuleException() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminPrincipalOrgA));
        when(userRepository.findById(5L)).thenReturn(Optional.of(agentOrgA));

        UpdateUserPermissionsRequest request = new UpdateUserPermissionsRequest();
        request.setPermissionCodes(List.of("CREATE_FORM"));

        assertThrows(BusinessRuleException.class, () ->
                permissionService.updateUserPermissions(adminPrincipalOrgA, 5L, request)
        );
    }

    @Test
    @DisplayName("updateUserPermissions() - Échec : Admin secondaire d'une autre organisation")
    void updateUserPermissions_DifferentOrganization_ThrowsForbidden() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminPrincipalOrgA));
        when(userRepository.findById(4L)).thenReturn(Optional.of(adminSecondaireOrgB));

        UpdateUserPermissionsRequest request = new UpdateUserPermissionsRequest();
        request.setPermissionCodes(List.of("CREATE_FORM"));

        assertThrows(ForbiddenOperationException.class, () ->
                permissionService.updateUserPermissions(adminPrincipalOrgA, 4L, request)
        );
    }

    @Test
    @DisplayName("updateUserPermissions() - Succès : le Super Admin peut gérer n'importe quelle organisation")
    void updateUserPermissions_BySuperAdmin_CrossOrganization_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(superAdmin));
        when(userRepository.findById(4L)).thenReturn(Optional.of(adminSecondaireOrgB));
        when(permissionRepository.findByCode("CREATE_FORM")).thenReturn(Optional.of(createFormPermission));
        when(userPermissionRepository.findByUserId(4L)).thenReturn(List.of());

        UpdateUserPermissionsRequest request = new UpdateUserPermissionsRequest();
        request.setPermissionCodes(List.of("CREATE_FORM"));

        UserPermissionsResponse response = permissionService.updateUserPermissions(superAdmin, 4L, request);

        assertNotNull(response);
        verify(userPermissionRepository, times(1)).deleteByUserId(4L);
        verify(userPermissionRepository, times(1)).save(any(UserPermission.class));
    }

    @Test
    @DisplayName("updateUserPermissions() - Échec : la permission demandée n'existe pas en base")
    void updateUserPermissions_UnknownPermissionCode_ThrowsResourceNotFound() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminPrincipalOrgA));
        when(userRepository.findById(3L)).thenReturn(Optional.of(adminSecondaireOrgA));
        when(permissionRepository.findByCode("CREATE_FORM")).thenReturn(Optional.empty());

        UpdateUserPermissionsRequest request = new UpdateUserPermissionsRequest();
        request.setPermissionCodes(List.of("CREATE_FORM"));

        assertThrows(ResourceNotFoundException.class, () ->
                permissionService.updateUserPermissions(adminPrincipalOrgA, 3L, request)
        );
    }

    @Test
    @DisplayName("hasPermission() - true si la permission vient du rôle")
    void hasPermission_FromRole_ReturnsTrue() {
        Role roleWithPermission = Role.builder().id(5L).name(RoleType.ADMIN_PRINCIPAL)
                .permissions(Set.of(createFormPermission)).build();
        User userWithRolePermission = User.builder().id(6L).role(roleWithPermission).build();

        when(userRepository.findById(6L)).thenReturn(Optional.of(userWithRolePermission));

        assertTrue(permissionService.hasPermission(userWithRolePermission, "CREATE_FORM"));
    }

    @Test
    @DisplayName("hasPermission() - true si la permission vient d'une délégation individuelle")
    void hasPermission_FromIndividualGrant_ReturnsTrue() {
        UserPermission grant = UserPermission.builder().user(adminSecondaireOrgA).permission(createFormPermission).build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(adminSecondaireOrgA));
        when(userPermissionRepository.findByUserId(3L)).thenReturn(List.of(grant));

        assertTrue(permissionService.hasPermission(adminSecondaireOrgA, "CREATE_FORM"));
    }

    @Test
    @DisplayName("hasPermission() - false si la permission n'est ni dans le rôle ni déléguée")
    void hasPermission_NotGranted_ReturnsFalse() {
        when(userRepository.findById(3L)).thenReturn(Optional.of(adminSecondaireOrgA));
        when(userPermissionRepository.findByUserId(3L)).thenReturn(List.of());

        assertFalse(permissionService.hasPermission(adminSecondaireOrgA, "CREATE_FORM"));
    }
}
