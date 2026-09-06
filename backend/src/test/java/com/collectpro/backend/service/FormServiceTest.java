package com.collectpro.backend.service;

import com.collectpro.backend.dto.FormResponse;
import com.collectpro.backend.entity.Form;
import com.collectpro.backend.entity.FormVersion;
import com.collectpro.backend.entity.Organization;
import com.collectpro.backend.entity.Role;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.AuditAction;
import com.collectpro.backend.enums.FormStatus;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.enums.UserStatus;
import com.collectpro.backend.exception.BusinessRuleException;
import com.collectpro.backend.exception.ForbiddenOperationException;
import com.collectpro.backend.repository.FormRepository;
import com.collectpro.backend.repository.FormVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FormServiceTest {

    @Mock
    private FormRepository formRepository;

    @Mock
    private FormVersionRepository formVersionRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private FormService formService;

    private User admin;
    private Organization org;
    private Form form;

    @BeforeEach
    void setUp() {
        org = Organization.builder().id(10L).name("Org Test").build();
        Role adminRole = Role.builder().id(2L).name(RoleType.ADMIN_PRINCIPAL).build();
        admin = User.builder().id(1L).firstName("Admin").lastName("User").email("admin@test.com")
                .role(adminRole).organization(org).build();
        form = Form.builder().id(50L).name("Enquête Santé").status(FormStatus.DRAFT).organization(org).build();
    }

    @Test
    @DisplayName("publishForm - Succès si version présente et log d'audit FORM_PUBLISHED")
    void publishForm_Success() {
        FormVersion version = FormVersion.builder().id(1L).form(form).versionNumber(1).build();

        when(formRepository.findById(50L)).thenReturn(Optional.of(form));
        when(formVersionRepository.findFirstByFormOrderByVersionNumberDesc(form)).thenReturn(Optional.of(version));
        when(formRepository.save(any(Form.class))).thenAnswer(inv -> inv.getArgument(0));

        FormResponse response = formService.publishForm(admin, 50L);

        assertNotNull(response);
        assertEquals(FormStatus.PUBLISHED, response.getStatus());
        verify(auditLogService).log(
                eq(admin),
                eq(org),
                eq(AuditAction.FORM_PUBLISHED),
                eq("Form"),
                eq(50L),
                anyString()
        );
    }

    @Test
    @DisplayName("publishForm - Échec si aucune version de formulaire n'existe")
    void publishForm_WithoutVersion_ThrowsException() {
        when(formRepository.findById(50L)).thenReturn(Optional.of(form));
        when(formVersionRepository.findFirstByFormOrderByVersionNumberDesc(form)).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class, () -> formService.publishForm(admin, 50L));
        verify(auditLogService, never()).log(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("publishForm - Échec si le formulaire appartient à une autre organisation (IDOR)")
    void publishForm_DifferentOrganization_ThrowsForbidden() {
        Organization otherOrg = Organization.builder().id(99L).name("Autre Org").build();
        Role adminRole = Role.builder().id(2L).name(RoleType.ADMIN_PRINCIPAL).build();
        User otherOrgAdmin = User.builder().id(2L).email("admin2@test.com")
                .role(adminRole).organization(otherOrg).build();

        when(formRepository.findById(50L)).thenReturn(Optional.of(form));

        assertThrows(ForbiddenOperationException.class,
                () -> formService.publishForm(otherOrgAdmin, 50L));
        verify(formRepository, never()).save(any());
        verify(auditLogService, never()).log(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("archiveForm - Succès et log d'audit FORM_ARCHIVED")
    void archiveForm_Success() {
        when(formRepository.findById(50L)).thenReturn(Optional.of(form));
        when(formRepository.save(any(Form.class))).thenAnswer(inv -> inv.getArgument(0));

        FormResponse response = formService.archiveForm(admin, 50L);

        assertEquals(FormStatus.ARCHIVED, response.getStatus());
        verify(auditLogService).log(
                eq(admin), eq(org), eq(AuditAction.FORM_ARCHIVED), eq("Form"), eq(50L), anyString());
    }

    @Test
    @DisplayName("archiveForm - Échec si le formulaire appartient à une autre organisation (IDOR)")
    void archiveForm_DifferentOrganization_ThrowsForbidden() {
        Organization otherOrg = Organization.builder().id(99L).name("Autre Org").build();
        Role adminRole = Role.builder().id(2L).name(RoleType.ADMIN_PRINCIPAL).build();
        User otherOrgAdmin = User.builder().id(2L).email("admin2@test.com")
                .role(adminRole).organization(otherOrg).build();

        when(formRepository.findById(50L)).thenReturn(Optional.of(form));

        assertThrows(ForbiddenOperationException.class,
                () -> formService.archiveForm(otherOrgAdmin, 50L));
        verify(formRepository, never()).save(any());
    }

    @Test
    @DisplayName("publishForm - Le Super Admin (sans organisation) n'est jamais restreint")
    void superAdmin_BypassesOrganizationCheck() {
        Role superAdminRole = Role.builder().id(3L).name(RoleType.SUPER_ADMIN).build();
        User superAdmin = User.builder().id(3L).email("super@test.com")
                .role(superAdminRole).organization(null).build();
        FormVersion version = FormVersion.builder().id(1L).form(form).versionNumber(1).build();

        when(formRepository.findById(50L)).thenReturn(Optional.of(form));
        when(formVersionRepository.findFirstByFormOrderByVersionNumberDesc(form)).thenReturn(Optional.of(version));
        when(formRepository.save(any(Form.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> formService.publishForm(superAdmin, 50L));
    }
}
