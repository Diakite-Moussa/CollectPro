package com.collectpro.backend.service;

import com.collectpro.backend.dto.AuditLogResponse;
import com.collectpro.backend.entity.AuditLog;
import com.collectpro.backend.entity.Organization;
import com.collectpro.backend.entity.Role;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.AuditAction;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.exception.ResourceNotFoundException;
import com.collectpro.backend.repository.AuditLogRepository;
import com.collectpro.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogQueryServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuditLogQueryService auditLogQueryService;

    private Organization org;
    private User superAdmin;
    private User admin;

    @BeforeEach
    void setUp() {
        org = Organization.builder().id(10L).name("Org Test").build();

        Role superAdminRole = Role.builder().id(1L).name(RoleType.SUPER_ADMIN).build();
        superAdmin = User.builder().id(1L).role(superAdminRole).build();

        Role adminRole = Role.builder().id(2L).name(RoleType.ADMIN_PRINCIPAL).build();
        admin = User.builder().id(2L).organization(org).role(adminRole).build();
    }

    private final Pageable pageable = PageRequest.of(0, 20);

    @Test
    @DisplayName("getAuditLogsForUser - SUPER_ADMIN voit tous les logs, toutes organisations confondues")
    void getAuditLogsForUser_SuperAdmin_ReturnsAllLogs() {
        AuditLog log = AuditLog.builder()
                .id(100L)
                .action(AuditAction.ORGANIZATION_CREATED)
                .entityType("Organization")
                .entityId(10L)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(superAdmin));
        when(auditLogRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(log)));

        Page<AuditLogResponse> result = auditLogQueryService.getAuditLogsForUser(superAdmin, pageable);

        assertEquals(1, result.getContent().size());
        assertEquals(AuditAction.ORGANIZATION_CREATED, result.getContent().get(0).getAction());
        verify(auditLogRepository).findAllByOrderByCreatedAtDesc(any(Pageable.class));
        verify(auditLogRepository, never()).findByOrganizationIdOrderByCreatedAtDesc(any(), any());
    }

    @Test
    @DisplayName("getAuditLogsForUser - Admin d'organisation ne voit que les logs de son organisation")
    void getAuditLogsForUser_OrgAdmin_ReturnsScopedLogs() {
        AuditLog log = AuditLog.builder()
                .id(101L)
                .action(AuditAction.USER_DISABLED)
                .entityType("User")
                .entityId(5L)
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(auditLogRepository.findByOrganizationIdOrderByCreatedAtDesc(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        Page<AuditLogResponse> result = auditLogQueryService.getAuditLogsForUser(admin, pageable);

        assertEquals(1, result.getContent().size());
        verify(auditLogRepository).findByOrganizationIdOrderByCreatedAtDesc(eq(10L), any(Pageable.class));
        verify(auditLogRepository, never()).findAllByOrderByCreatedAtDesc(any());
    }

    @Test
    @DisplayName("getAuditLogsForUser - Utilisateur sans organisation retourne une liste vide, pas d'exception")
    void getAuditLogsForUser_NoOrganization_ReturnsEmptyList() {
        Role role = Role.builder().id(3L).name(RoleType.ADMIN_SECONDAIRE).build();
        User userWithoutOrg = User.builder().id(3L).organization(null).role(role).build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(userWithoutOrg));

        Page<AuditLogResponse> result = auditLogQueryService.getAuditLogsForUser(userWithoutOrg, pageable);

        assertTrue(result.isEmpty());
        verifyNoInteractions(auditLogRepository);
    }

    @Test
    @DisplayName("getAuditLogsForUser - Utilisateur introuvable lève ResourceNotFoundException")
    void getAuditLogsForUser_UserNotFound_ThrowsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        User ghost = User.builder().id(99L).build();

        assertThrows(ResourceNotFoundException.class,
                () -> auditLogQueryService.getAuditLogsForUser(ghost, pageable));
    }
}