package com.collectpro.backend.service;

import com.collectpro.backend.dto.CreateFormVersionRequest;
import com.collectpro.backend.dto.FormVersionResponse;
import com.collectpro.backend.entity.Form;
import com.collectpro.backend.entity.FormVersion;
import com.collectpro.backend.entity.Organization;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.repository.FormVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FormVersionServiceTest {

    @Mock
    private FormVersionRepository formVersionRepository;

    @Mock
    private FormService formService;

    @InjectMocks
    private FormVersionService formVersionService;

    private Form form;
    private User creator;

    @BeforeEach
    void setUp() {
        Organization org = Organization.builder().id(10L).name("Org Test").build();
        form = Form.builder().id(50L).name("Enquête Santé").organization(org).build();
        creator = User.builder().id(1L).firstName("Admin").organization(org).build();
    }

    @Test
    @DisplayName("createVersion - Première version d'un formulaire démarre à 1")
    void createVersion_FirstVersion_StartsAtOne() {
        CreateFormVersionRequest request = new CreateFormVersionRequest();
        request.setSchemaJson("{\"fields\":[]}");

        when(formService.getFormEntity(50L)).thenReturn(form);
        when(formVersionRepository.findFirstByFormOrderByVersionNumberDesc(form)).thenReturn(Optional.empty());
        when(formVersionRepository.save(any(FormVersion.class))).thenAnswer(inv -> {
            FormVersion v = inv.getArgument(0);
            v.setId(1L);
            return v;
        });

        FormVersionResponse response = formVersionService.createVersion(50L, request, creator);

        assertEquals(1, response.getVersionNumber());
        assertEquals(50L, response.getFormId());
    }

    @Test
    @DisplayName("createVersion - Version suivante incrémente le numéro existant")
    void createVersion_NextVersion_Increments() {
        CreateFormVersionRequest request = new CreateFormVersionRequest();
        request.setSchemaJson("{\"fields\":[]}");

        FormVersion existing = FormVersion.builder().id(1L).form(form).versionNumber(2).build();

        when(formService.getFormEntity(50L)).thenReturn(form);
        when(formVersionRepository.findFirstByFormOrderByVersionNumberDesc(form)).thenReturn(Optional.of(existing));
        when(formVersionRepository.save(any(FormVersion.class))).thenAnswer(inv -> {
            FormVersion v = inv.getArgument(0);
            v.setId(2L);
            return v;
        });

        FormVersionResponse response = formVersionService.createVersion(50L, request, creator);

        assertEquals(3, response.getVersionNumber());
    }

    @Test
    @DisplayName("getVersionsForForm - Retourne les versions du formulaire")
    void getVersionsForForm_ReturnsList() {
        FormVersion v1 = FormVersion.builder().id(1L).form(form).versionNumber(1).createdBy(creator).build();
        FormVersion v2 = FormVersion.builder().id(2L).form(form).versionNumber(2).createdBy(creator).build();

        when(formService.getFormEntity(50L)).thenReturn(form);
        when(formVersionRepository.findByFormOrderByVersionNumberDesc(form)).thenReturn(List.of(v2, v1));

        List<FormVersionResponse> result = formVersionService.getVersionsForForm(50L);

        assertEquals(2, result.size());
        assertEquals(2, result.get(0).getVersionNumber());
    }
}