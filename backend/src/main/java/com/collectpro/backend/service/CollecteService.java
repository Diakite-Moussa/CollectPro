package com.collectpro.backend.service;

import com.collectpro.backend.dto.CollecteAttachmentResponse;
import com.collectpro.backend.dto.CollecteResponse;
import com.collectpro.backend.dto.CreateCollecteRequest;
import com.collectpro.backend.dto.RejectCollecteRequest;
import com.collectpro.backend.dto.ValidateCollecteRequest;
import com.collectpro.backend.entity.Collecte;
import com.collectpro.backend.entity.CollecteAttachment;
import com.collectpro.backend.entity.FormVersion;
import com.collectpro.backend.entity.SupervisorAgent;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.CollecteStatus;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.exception.BusinessRuleException;
import com.collectpro.backend.exception.ForbiddenOperationException;
import com.collectpro.backend.exception.ResourceNotFoundException;
import com.collectpro.backend.repository.CollecteAttachmentRepository;
import com.collectpro.backend.repository.CollecteRepository;
import com.collectpro.backend.repository.SupervisorAgentRepository;
import com.collectpro.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.collectpro.backend.dto.ValidationResponse;
import com.collectpro.backend.entity.Validation;
import com.collectpro.backend.enums.AuditAction;
import com.collectpro.backend.enums.ValidationDecision;
import com.collectpro.backend.repository.ValidationRepository;

import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CollecteService {

    private final CollecteRepository collecteRepository;
    private final SupervisorAgentRepository supervisorAgentRepository;
    private final FormVersionService formVersionService;
    private final UserRepository userRepository;
    private final CollecteAttachmentService attachmentService;
    private final CollecteAttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;
    private final ValidationRepository validationRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public CollecteResponse createCollecte(CreateCollecteRequest request, User agent) {
        return createCollecte(request, agent, List.of());
    }

    @Transactional
    public CollecteResponse createCollecte(CreateCollecteRequest request, User agent, List<MultipartFile> files) {
        FormVersion formVersion = formVersionService.getVersionEntity(request.getFormVersionId());

        if (!formVersion.getForm().getOrganization().getId().equals(agent.getOrganization().getId())) {
            throw new ForbiddenOperationException(
                    "Ce formulaire n'appartient pas à l'organisation de l'agent");
        }

        Collecte collecte = Collecte.builder()
                .agent(agent)
                .formVersion(formVersion)
                .dataJson(request.getDataJson())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .status(CollecteStatus.PENDING_VALIDATION)
                .build();
        collecte = collecteRepository.save(collecte);

        String cleanedJson = attachmentService.migrateLegacyBase64Photos(collecte, collecte.getDataJson());
        if (!cleanedJson.equals(collecte.getDataJson())) {
            collecte.setDataJson(cleanedJson);
            collecte = collecteRepository.save(collecte);
        }
        attachmentService.storeUploadedFiles(collecte, files);

        return toResponse(collecte);
    }

    @Transactional(readOnly = true)
    public Resource loadAttachmentResource(Long attachmentId, User requester) {
        CollecteAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pièce jointe introuvable"));
        Collecte collecte = attachment.getCollecte();
        assertCanViewCollecte(requester, collecte);
        try {
            Path path = fileStorageService.resolvePath(attachment.getStoredFilename());
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("Fichier introuvable sur le disque");
            }
            return resource;
        } catch (Exception e) {
            throw new ResourceNotFoundException("Fichier introuvable");
        }
    }

    public List<CollecteResponse> getCollectesForAgent(User agent) {
        return collecteRepository.findByAgent(agent).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<CollecteResponse> getCollectesForSupervisor(User supervisor) {
        List<User> agents = supervisorAgentRepository.findBySupervisorId(supervisor.getId()).stream()
                .map(SupervisorAgent::getAgent)
                .toList();
        return collecteRepository.findByAgentIn(agents).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<CollecteResponse> getPendingValidationForSupervisor(User supervisor) {
        List<User> agents = supervisorAgentRepository.findBySupervisorId(supervisor.getId()).stream()
                .map(SupervisorAgent::getAgent)
                .toList();
        return collecteRepository.findByAgentInAndStatus(agents, CollecteStatus.PENDING_VALIDATION).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<CollecteResponse> getCollectesForAdmin(User admin) {
        User managedAdmin = userRepository.findById(admin.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        List<Collecte> collectes;
        if (managedAdmin.getRole().getName() == RoleType.SUPER_ADMIN) {
            collectes = collecteRepository.findAll();
        } else {
            if (managedAdmin.getOrganization() == null) {
                throw new BusinessRuleException("Utilisateur sans organisation");
            }
            collectes = collecteRepository.findByAgent_OrganizationId(managedAdmin.getOrganization().getId());
        }
        return collectes.stream().map(this::toResponse).toList();
    }

    @Transactional
    public CollecteResponse validateCollecte(Long collecteId, User supervisor, ValidateCollecteRequest request) {
        Collecte collecte = getCollecteAndCheckSupervision(collecteId, supervisor);

        collecte.setStatus(CollecteStatus.VALIDATED);
        collecte.setValidationComment(request.getComment());
        collecte.setValidatedBy(supervisor);
        collecte.setValidatedAt(java.time.LocalDateTime.now());
        collecte = collecteRepository.save(collecte);

        recordValidationHistory(collecte, supervisor, ValidationDecision.VALIDEE, request.getComment());

        auditLogService.log(
                supervisor,
                collecte.getAgent().getOrganization(),
                AuditAction.COLLECTE_VALIDATED,
                "Collecte",
                collecte.getId(),
                "Collecte #" + collecte.getId() + " validée (agent " + collecte.getAgent().getEmail() + ")"
        );

        return toResponse(collecte);
    }

    @Transactional
    public CollecteResponse rejectCollecte(Long collecteId, User supervisor, RejectCollecteRequest request) {
        Collecte collecte = getCollecteAndCheckSupervision(collecteId, supervisor);

        collecte.setStatus(CollecteStatus.REJECTED);
        collecte.setValidationComment(request.getComment());
        collecte.setValidatedBy(supervisor);
        collecte.setValidatedAt(java.time.LocalDateTime.now());
        collecte = collecteRepository.save(collecte);

        recordValidationHistory(collecte, supervisor, ValidationDecision.REJETEE, request.getComment());

        auditLogService.log(
                supervisor,
                collecte.getAgent().getOrganization(),
                AuditAction.COLLECTE_REJECTED,
                "Collecte",
                collecte.getId(),
                "Collecte #" + collecte.getId() + " rejetée (agent " + collecte.getAgent().getEmail() + ") : " + request.getComment()
        );

        return toResponse(collecte);
    }
    private Collecte getCollecteAndCheckSupervision(Long collecteId, User supervisor) {
        Collecte collecte = collecteRepository.findById(collecteId)
                .orElseThrow(() -> new ResourceNotFoundException("Collecte introuvable"));

        if (collecte.getStatus() != CollecteStatus.PENDING_VALIDATION) {
            throw new BusinessRuleException(
                    "Cette collecte a déjà été traitée (statut actuel : " + collecte.getStatus() + ")");
        }

        boolean isSupervisedByThisSupervisor = supervisorAgentRepository
                .findByAgentId(collecte.getAgent().getId())
                .map(sa -> sa.getSupervisor().getId().equals(supervisor.getId()))
                .orElse(false);

        if (!isSupervisedByThisSupervisor) {
            throw new ForbiddenOperationException(
                    "Vous ne supervisez pas l'agent auteur de cette collecte");
        }

        return collecte;
    }

    private void recordValidationHistory(Collecte collecte, User supervisor, ValidationDecision decision, String comment) {
        Validation validation = Validation.builder()
                .collecte(collecte)
                .supervisor(supervisor)
                .decision(decision)
                .comment(comment)
                .build();
        validationRepository.save(validation);
    }

    @Transactional(readOnly = true)
    public List<ValidationResponse> getValidationHistory(Long collecteId, User requester) {
        Collecte collecte = collecteRepository.findById(collecteId)
                .orElseThrow(() -> new ResourceNotFoundException("Collecte introuvable"));
        assertCanViewCollecte(requester, collecte);

        return validationRepository.findByCollecteIdOrderByCreatedAtDesc(collecteId).stream()
                .map(v -> ValidationResponse.builder()
                        .id(v.getId())
                        .decision(v.getDecision())
                        .comment(v.getComment())
                        .createdAt(v.getCreatedAt())
                        .supervisor(ValidationResponse.SupervisorSummary.builder()
                                .id(v.getSupervisor().getId())
                                .firstName(v.getSupervisor().getFirstName())
                                .lastName(v.getSupervisor().getLastName())
                                .build())
                        .build())
                .toList();
    }

    private void assertCanViewCollecte(User requester, Collecte collecte) {
        User managed = userRepository.findById(requester.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        RoleType role = managed.getRole().getName();

        if (role == RoleType.SUPER_ADMIN) {
            return;
        }
        if (role == RoleType.AGENT && collecte.getAgent().getId().equals(managed.getId())) {
            return;
        }
        if (role == RoleType.SUPERVISOR) {
            boolean supervises = supervisorAgentRepository.findByAgentId(collecte.getAgent().getId())
                    .map(sa -> sa.getSupervisor().getId().equals(managed.getId()))
                    .orElse(false);
            if (supervises) return;
        }
        if (managed.getOrganization() != null
                && collecte.getAgent().getOrganization() != null
                && managed.getOrganization().getId().equals(collecte.getAgent().getOrganization().getId())
                && (role == RoleType.ADMIN_PRINCIPAL || role == RoleType.ADMIN_SECONDAIRE)) {
            return;
        }
        throw new ForbiddenOperationException("Accès refusé à cette pièce jointe");
    }

    private CollecteResponse toResponse(Collecte collecte) {
        List<CollecteAttachmentResponse> attachments =
                attachmentService.getAttachmentsForCollecte(collecte.getId());

        return CollecteResponse.builder()
                .id(collecte.getId())
                .agent(CollecteResponse.AgentSummary.builder()
                        .id(collecte.getAgent().getId())
                        .firstName(collecte.getAgent().getFirstName())
                        .lastName(collecte.getAgent().getLastName())
                        .build())
                .formVersionId(collecte.getFormVersion().getId())
                .dataJson(collecte.getDataJson())
                .attachments(attachments)
                .latitude(collecte.getLatitude())
                .longitude(collecte.getLongitude())
                .status(collecte.getStatus())
                .validationComment(collecte.getValidationComment())
                .validatedBy(collecte.getValidatedBy() != null
                        ? CollecteResponse.AgentSummary.builder()
                        .id(collecte.getValidatedBy().getId())
                        .firstName(collecte.getValidatedBy().getFirstName())
                        .lastName(collecte.getValidatedBy().getLastName())
                        .build()
                        : null)
                .validatedAt(collecte.getValidatedAt())
                .createdAt(collecte.getCreatedAt())
                .updatedAt(collecte.getUpdatedAt())
                .build();
    }
}
