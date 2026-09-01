package com.collectpro.backend.service;

import com.collectpro.backend.dto.CreateOrganizationRequest;
import com.collectpro.backend.dto.OrganizationResponse;
import com.collectpro.backend.dto.UpdateOrganizationRequest;
import com.collectpro.backend.dto.UpdateOrganizationStatusRequest;
import com.collectpro.backend.entity.Organization;
import com.collectpro.backend.entity.Role;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.AuditAction;
import com.collectpro.backend.enums.OrganizationStatus;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.enums.UserStatus;
import com.collectpro.backend.exception.BusinessRuleException;
import com.collectpro.backend.exception.ResourceNotFoundException;
import com.collectpro.backend.repository.OrganizationRepository;
import com.collectpro.backend.repository.RoleRepository;
import com.collectpro.backend.repository.UserRepository;
import com.collectpro.backend.security.TokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TokenUtil tokenUtil;
    private final PasswordEncoder passwordEncoder;
    private final UserInvitationService userInvitationService;
    private final AuditLogService auditLogService;
    private final FileStorageService fileStorageService;

    @Transactional
    public OrganizationResponse createOrganizationWithPrincipalAdmin(User actor, CreateOrganizationRequest request) {

        if (userRepository.existsByEmail(request.getAdminEmail())) {
            throw new BusinessRuleException(
                    "Un utilisateur avec l'email '" + request.getAdminEmail() + "' existe déjà");
        }

        Organization organization = Organization.builder()
                .name(request.getName())
                .description(request.getDescription())
                .status(OrganizationStatus.ACTIVE)
                .build();
        organization = organizationRepository.save(organization);

        Role adminPrincipalRole = roleRepository.findByName(RoleType.ADMIN_PRINCIPAL)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rôle ADMIN_PRINCIPAL introuvable — vérifier l'initialisation des rôles"));

        User admin = User.builder()
                .firstName(request.getAdminFirstName())
                .lastName(request.getAdminLastName())
                .email(request.getAdminEmail())
                .phone(request.getAdminPhone())
                .password(passwordEncoder.encode(tokenUtil.generateRawToken()))
                .status(UserStatus.INVITED)
                .role(adminPrincipalRole)
                .organization(organization)
                .build();
        admin = userRepository.save(admin);

        userInvitationService.sendInvitation(admin);

        auditLogService.logAfterCommit(
                actor,
                organization,
                AuditAction.ORGANIZATION_CREATED,
                "Organization",
                organization.getId(),
                "Création de l'organisation '" + organization.getName() + "' avec Admin principal " + admin.getEmail()
        );

        return toResponse(organization, admin);
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> getAllOrganizations() {
        return organizationRepository.findAll().stream()
                .map(org -> {
                    User principalAdmin = userRepository
                            .findByOrganizationIdAndRole_Name(org.getId(), RoleType.ADMIN_PRINCIPAL)
                            .orElse(null);
                    return toResponse(org, principalAdmin);
                })
                .toList();
    }

    @Transactional
    public OrganizationResponse updateOrganization(User actor, Long id, UpdateOrganizationRequest request) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation introuvable"));

        organization.setName(request.getName());
        organization.setDescription(request.getDescription());
        organization = organizationRepository.save(organization);

        auditLogService.log(
                actor,
                organization,
                AuditAction.ORGANIZATION_UPDATED,
                "Organization",
                organization.getId(),
                "Modification de l'organisation '" + organization.getName() + "'"
        );

        User principalAdmin = userRepository
                .findByOrganizationIdAndRole_Name(organization.getId(), RoleType.ADMIN_PRINCIPAL)
                .orElse(null);
        return toResponse(organization, principalAdmin);
    }

    @Transactional
    public OrganizationResponse updateOrganizationStatus(User actor, Long id, UpdateOrganizationStatusRequest request) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation introuvable"));

        organization.setStatus(request.getStatus());
        organization = organizationRepository.save(organization);

        auditLogService.log(
                actor,
                organization,
                AuditAction.ORGANIZATION_UPDATED,
                "Organization",
                organization.getId(),
                "Changement de statut de l'organisation '" + organization.getName() + "' → " + request.getStatus()
        );

        User principalAdmin = userRepository
                .findByOrganizationIdAndRole_Name(organization.getId(), RoleType.ADMIN_PRINCIPAL)
                .orElse(null);
        return toResponse(organization, principalAdmin);
    }

    @Transactional
    public OrganizationResponse updateLogo(User actor, Long id, MultipartFile file) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation introuvable"));

        if (file.isEmpty()) {
            throw new BusinessRuleException("Fichier vide");
        }
        String contentType = file.getContentType();
        if (contentType == null || !(contentType.equals("image/png") || contentType.equals("image/jpeg"))) {
            throw new BusinessRuleException("Format de logo non supporté (PNG ou JPEG uniquement)");
        }

        try {
            FileStorageService.StoredFile stored = fileStorageService.saveOrganizationLogo(id, file);
            organization.setLogoUrl(stored.storedFilename());
            organization.setLogoContentType(stored.contentType());
        } catch (IOException e) {
            throw new BusinessRuleException("Erreur lors de l'enregistrement du logo");
        }

        organization = organizationRepository.save(organization);

        auditLogService.log(
                actor,
                organization,
                AuditAction.ORGANIZATION_UPDATED,
                "Organization",
                organization.getId(),
                "Mise à jour du logo de l'organisation '" + organization.getName() + "'"
        );

        User principalAdmin = userRepository
                .findByOrganizationIdAndRole_Name(organization.getId(), RoleType.ADMIN_PRINCIPAL)
                .orElse(null);
        return toResponse(organization, principalAdmin);
    }

    @Transactional(readOnly = true)
    public LogoFile loadLogoFile(Long organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation introuvable"));
        if (organization.getLogoUrl() == null) {
            throw new ResourceNotFoundException("Logo non défini pour cette organisation");
        }
        try {
            Path path = fileStorageService.resolvePath(organization.getLogoUrl());
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("Fichier introuvable sur le disque");
            }
            String contentType = organization.getLogoContentType() != null
                    ? organization.getLogoContentType()
                    : "application/octet-stream";
            return new LogoFile(resource, contentType);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new ResourceNotFoundException("Fichier introuvable");
        }
    }

    public record LogoFile(Resource resource, String contentType) {}

    private OrganizationResponse toResponse(Organization organization, User admin) {
        return OrganizationResponse.builder()
                .id(organization.getId())
                .name(organization.getName())
                .description(organization.getDescription())
                .status(organization.getStatus())
                .createdAt(organization.getCreatedAt())
                .logoUrl(organization.getLogoUrl() != null ? "/files/organizations/" + organization.getId() + "/logo" : null)
                .principalAdmin(admin == null ? null : OrganizationResponse.UserSummary.builder()
                        .id(admin.getId())
                        .firstName(admin.getFirstName())
                        .lastName(admin.getLastName())
                        .email(admin.getEmail())
                        .status(admin.getStatus().name())
                        .build())
                .build();
    }
}