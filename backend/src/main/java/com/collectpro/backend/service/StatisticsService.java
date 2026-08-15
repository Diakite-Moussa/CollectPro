package com.collectpro.backend.service;

import com.collectpro.backend.dto.StatisticsResponse;
import com.collectpro.backend.entity.SupervisorAgent;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.CollecteStatus;
import com.collectpro.backend.enums.FormStatus;
import com.collectpro.backend.enums.OrganizationStatus;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.exception.ResourceNotFoundException;
import com.collectpro.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final FormRepository formRepository;
    private final CollecteRepository collecteRepository;
    private final SupervisorAgentRepository supervisorAgentRepository;

    @Transactional(readOnly = true)
    public StatisticsResponse getStatistics(User requester) {
        User managed = userRepository.findById(requester.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        RoleType role = managed.getRole().getName();

        return switch (role) {
            case SUPER_ADMIN -> buildGlobalStatistics();
            case ADMIN_PRINCIPAL, ADMIN_SECONDAIRE -> buildOrganizationStatistics(managed);
            case SUPERVISOR -> buildTeamStatistics(managed);
            default -> throw new ResourceNotFoundException(
                    "Aucune statistique disponible pour ce rôle");
        };
    }

    private StatisticsResponse buildGlobalStatistics() {
        return StatisticsResponse.builder()
                .scope("GLOBAL")
                .totalOrganizations(organizationRepository.count())
                .activeOrganizations(organizationRepository.countByStatus(OrganizationStatus.ACTIVE))
                .totalUsers(userRepository.count())
                .totalAgents(userRepository.countByRole_Name(RoleType.AGENT))
                .totalSupervisors(userRepository.countByRole_Name(RoleType.SUPERVISOR))
                .totalAdmins(userRepository.countByRole_Name(RoleType.ADMIN_PRINCIPAL)
                        + userRepository.countByRole_Name(RoleType.ADMIN_SECONDAIRE))
                .totalForms(formRepository.count())
                .publishedForms(formRepository.countByStatus(FormStatus.PUBLISHED))
                .totalCollectes(collecteRepository.count())
                .pendingCollectes(collecteRepository.countByStatus(CollecteStatus.PENDING_VALIDATION))
                .validatedCollectes(collecteRepository.countByStatus(CollecteStatus.VALIDATED))
                .rejectedCollectes(collecteRepository.countByStatus(CollecteStatus.REJECTED))
                .build();
    }

    private StatisticsResponse buildOrganizationStatistics(User admin) {
        if (admin.getOrganization() == null) {
            throw new ResourceNotFoundException("Utilisateur sans organisation");
        }
        Long orgId = admin.getOrganization().getId();

        return StatisticsResponse.builder()
                .scope("ORGANIZATION")
                .organizationId(orgId)
                .organizationName(admin.getOrganization().getName())
                .totalUsers(userRepository.countByOrganizationId(orgId))
                .totalAgents(userRepository.countByOrganizationIdAndRole_Name(orgId, RoleType.AGENT))
                .totalSupervisors(userRepository.countByOrganizationIdAndRole_Name(orgId, RoleType.SUPERVISOR))
                .totalAdmins(userRepository.countByOrganizationIdAndRole_Name(orgId, RoleType.ADMIN_PRINCIPAL)
                        + userRepository.countByOrganizationIdAndRole_Name(orgId, RoleType.ADMIN_SECONDAIRE))
                .totalForms(formRepository.countByOrganization_Id(orgId))
                .publishedForms(formRepository.countByOrganization_IdAndStatus(orgId, FormStatus.PUBLISHED))
                .totalCollectes(collecteRepository.countByAgent_OrganizationId(orgId))
                .pendingCollectes(collecteRepository.countByAgent_OrganizationIdAndStatus(orgId, CollecteStatus.PENDING_VALIDATION))
                .validatedCollectes(collecteRepository.countByAgent_OrganizationIdAndStatus(orgId, CollecteStatus.VALIDATED))
                .rejectedCollectes(collecteRepository.countByAgent_OrganizationIdAndStatus(orgId, CollecteStatus.REJECTED))
                .build();
    }

    private StatisticsResponse buildTeamStatistics(User supervisor) {
        List<User> agents = supervisorAgentRepository.findBySupervisorId(supervisor.getId()).stream()
                .map(SupervisorAgent::getAgent)
                .toList();

        Long orgId = supervisor.getOrganization() != null ? supervisor.getOrganization().getId() : null;

        return StatisticsResponse.builder()
                .scope("TEAM")
                .organizationId(orgId)
                .organizationName(supervisor.getOrganization() != null ? supervisor.getOrganization().getName() : null)
                .totalUsers((long) agents.size())
                .totalAgents((long) agents.size())
                .totalSupervisors(0L)
                .totalAdmins(0L)
                .totalForms(orgId != null ? formRepository.countByOrganization_Id(orgId) : 0L)
                .publishedForms(orgId != null ? formRepository.countByOrganization_IdAndStatus(orgId, FormStatus.PUBLISHED) : 0L)
                .totalCollectes(collecteRepository.countByAgentIn(agents))
                .pendingCollectes(collecteRepository.countByAgentInAndStatus(agents, CollecteStatus.PENDING_VALIDATION))
                .validatedCollectes(collecteRepository.countByAgentInAndStatus(agents, CollecteStatus.VALIDATED))
                .rejectedCollectes(collecteRepository.countByAgentInAndStatus(agents, CollecteStatus.REJECTED))
                .build();
    }
}