package com.collectpro.backend.service;

import com.collectpro.backend.dto.FormResponse;
import com.collectpro.backend.entity.Form;
import com.collectpro.backend.entity.FormVersion;
import com.collectpro.backend.entity.Organization;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.AuditAction;
import com.collectpro.backend.enums.FormStatus;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.enums.UserStatus;
import com.collectpro.backend.exception.BusinessRuleException;
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
        admin = User.builder().id(1L).firstName("Admin").lastName("User").email("admin@test.com").organization(org).build();
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
}
