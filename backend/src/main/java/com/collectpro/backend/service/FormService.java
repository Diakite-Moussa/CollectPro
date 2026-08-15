package com.collectpro.backend.service;

import com.collectpro.backend.dto.CreateFormRequest;
import com.collectpro.backend.dto.FormResponse;
import com.collectpro.backend.entity.Form;
import com.collectpro.backend.entity.Organization;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.AuditAction;
import com.collectpro.backend.enums.FormStatus;
import com.collectpro.backend.exception.BusinessRuleException;
import com.collectpro.backend.exception.ResourceNotFoundException;
import com.collectpro.backend.repository.FormRepository;
import com.collectpro.backend.repository.FormVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FormService {

    private final FormRepository formRepository;
    private final FormVersionRepository formVersionRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public FormResponse createForm(CreateFormRequest request, Organization organization) {
        Form form = Form.builder()
                .name(request.getName())
                .description(request.getDescription())
                .organization(organization)
                .status(FormStatus.DRAFT)
                .build();
        form = formRepository.save(form);
        return toResponse(form);
    }

    public Form getFormEntity(Long formId) {
        return formRepository.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Formulaire introuvable (id=" + formId + ")"));
    }

    public List<FormResponse> getFormsForOrganization(Organization organization) {
        if (organization == null) {
            return java.util.Collections.emptyList();
        }
        return formRepository.findByOrganization(organization).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<FormResponse> getPublishedFormsForOrganization(Organization organization) {
        if (organization == null) {
            return java.util.Collections.emptyList();
        }
        return formRepository.findByOrganizationAndStatus(organization, FormStatus.PUBLISHED).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public FormResponse publishForm(User actor, Long formId) {
        Form form = getFormEntity(formId);

        if (form.getStatus() == FormStatus.ARCHIVED) {
            throw new BusinessRuleException("Un formulaire archivé ne peut pas être publié");
        }
        if (formVersionRepository.findFirstByFormOrderByVersionNumberDesc(form).isEmpty()) {
            throw new BusinessRuleException("Impossible de publier un formulaire sans version");
        }

        form.setStatus(FormStatus.PUBLISHED);
        form = formRepository.save(form);

        auditLogService.log(
                actor,
                form.getOrganization(),
                AuditAction.FORM_PUBLISHED,
                "Form",
                form.getId(),
                "Publication du formulaire '" + form.getName() + "'"
        );

        return toResponse(form);
    }

    @Transactional
    public FormResponse archiveForm(Long formId) {
        Form form = getFormEntity(formId);
        form.setStatus(FormStatus.ARCHIVED);
        form = formRepository.save(form);
        return toResponse(form);
    }

    private FormResponse toResponse(Form form) {
        Integer latestVersion = formVersionRepository.findFirstByFormOrderByVersionNumberDesc(form)
                .map(v -> v.getVersionNumber())
                .orElse(null);

        return FormResponse.builder()
                .id(form.getId())
                .name(form.getName())
                .description(form.getDescription())
                .status(form.getStatus())
                .createdAt(form.getCreatedAt())
                .organizationId(form.getOrganization().getId())
                .latestVersionNumber(latestVersion)
                .build();
    }
}