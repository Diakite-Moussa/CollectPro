package com.collectpro.backend.service;

import com.collectpro.backend.dto.CreateFormVersionRequest;
import com.collectpro.backend.dto.FormVersionResponse;
import com.collectpro.backend.entity.Form;
import com.collectpro.backend.entity.FormVersion;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.exception.ResourceNotFoundException;
import com.collectpro.backend.repository.FormVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FormVersionService {

    private final FormVersionRepository formVersionRepository;
    private final FormService formService;

    @Transactional
    public FormVersionResponse createVersion(Long formId, CreateFormVersionRequest request, User createdBy) {
        Form form = formService.getFormEntity(formId);
        formService.assertSameOrganization(form, createdBy);

        int nextVersionNumber = formVersionRepository.findFirstByFormOrderByVersionNumberDesc(form)
                .map(v -> v.getVersionNumber() + 1)
                .orElse(1);

        FormVersion version = FormVersion.builder()
                .form(form)
                .versionNumber(nextVersionNumber)
                .schemaJson(request.getSchemaJson())
                .createdBy(createdBy)
                .build();
        version = formVersionRepository.save(version);
        return toResponse(version);
    }

    public FormVersion getVersionEntity(Long versionId) {
        return formVersionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Version de formulaire introuvable (id=" + versionId + ")"));
    }

    public FormVersionResponse getVersionResponse(Long versionId, User actor) {
        FormVersion version = getVersionEntity(versionId);
        formService.assertSameOrganization(version.getForm(), actor);
        return toResponse(version);
    }

    public List<FormVersionResponse> getVersionsForForm(Long formId, User actor) {
        Form form = formService.getFormEntity(formId);
        formService.assertSameOrganization(form, actor);
        return formVersionRepository.findByFormOrderByVersionNumberDesc(form).stream()
                .map(this::toResponse)
                .toList();
    }

    private FormVersionResponse toResponse(FormVersion version) {
        return FormVersionResponse.builder()
                .id(version.getId())
                .formId(version.getForm().getId())
                .versionNumber(version.getVersionNumber())
                .schemaJson(version.getSchemaJson())
                .createdAt(version.getCreatedAt())
                .createdById(version.getCreatedBy().getId())
                .build();
    }
}