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
import com.collectpro.backend.repository.projection.AgentRejectionProjection;
import com.collectpro.backend.repository.projection.DailyCountProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    public StatisticsResponse getStatistics(User requester, int trendDays) {
        User managed = userRepository.findById(requester.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        RoleType role = managed.getRole().getName();

        return switch (role) {
            case SUPER_ADMIN -> buildGlobalStatistics(trendDays);
            case ADMIN_PRINCIPAL, ADMIN_SECONDAIRE -> buildOrganizationStatistics(managed, trendDays);
            case SUPERVISOR -> buildTeamStatistics(managed, trendDays);
            default -> throw new ResourceNotFoundException(
                    "Aucune statistique disponible pour ce rôle");
        };
    }

    private StatisticsResponse buildGlobalStatistics(int trendDays) {
        LocalDateTime since = LocalDateTime.now().minusDays(trendDays);

        List<StatisticsResponse.DailyCountEntry> trend =
                collecteRepository.findDailyCountsGlobal(since).stream()
                        .map(this::toDailyCountEntry)
                        .toList();

        List<StatisticsResponse.AgentRejectionRate> rejection =
                collecteRepository.findRejectionRatesGlobal().stream()
                        .map(this::toAgentRejectionRate)
                        .toList();

        Double avgValidationHours = collecteRepository.findAvgValidationTimeHoursGlobal();

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
                .collecteTrend(trend)
                .rejectionByAgent(rejection)
                .avgValidationTimeHours(avgValidationHours)
                .build();
    }

    private StatisticsResponse buildOrganizationStatistics(User admin, int trendDays) {
        if (admin.getOrganization() == null) {
            throw new ResourceNotFoundException("Utilisateur sans organisation");
        }
        Long orgId = admin.getOrganization().getId();
        LocalDateTime since = LocalDateTime.now().minusDays(trendDays);

        List<StatisticsResponse.DailyCountEntry> trend =
                collecteRepository.findDailyCountsByOrganization(orgId, since).stream()
                        .map(this::toDailyCountEntry)
                        .toList();

        List<StatisticsResponse.AgentRejectionRate> rejection =
                collecteRepository.findRejectionRatesByOrganization(orgId).stream()
                        .map(this::toAgentRejectionRate)
                        .toList();

        Double avgValidationHours = collecteRepository.findAvgValidationTimeHoursByOrganization(orgId);

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
                .collecteTrend(trend)
                .rejectionByAgent(rejection)
                .avgValidationTimeHours(avgValidationHours)
                .build();
    }

    private StatisticsResponse buildTeamStatistics(User supervisor, int trendDays) {
        List<User> agents = supervisorAgentRepository.findBySupervisorId(supervisor.getId()).stream()
                .map(SupervisorAgent::getAgent)
                .toList();
        List<Long> agentIds = agents.stream().map(User::getId).toList();

        Long orgId = supervisor.getOrganization() != null ? supervisor.getOrganization().getId() : null;
        LocalDateTime since = LocalDateTime.now().minusDays(trendDays);

        List<StatisticsResponse.DailyCountEntry> trend = agents.isEmpty()
                ? List.of()
                : collecteRepository.findDailyCountsByAgents(agents, since).stream()
                        .map(this::toDailyCountEntry)
                        .toList();

        List<StatisticsResponse.AgentRejectionRate> rejection = agents.isEmpty()
                ? List.of()
                : collecteRepository.findRejectionRatesByAgents(agents).stream()
                        .map(this::toAgentRejectionRate)
                        .toList();

        Double avgValidationHours = agentIds.isEmpty()
                ? null
                : collecteRepository.findAvgValidationTimeHoursByAgentIds(agentIds);

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
                .collecteTrend(trend)
                .rejectionByAgent(rejection)
                .avgValidationTimeHours(avgValidationHours)
                .build();
    }

    private StatisticsResponse.DailyCountEntry toDailyCountEntry(DailyCountProjection p) {
        return StatisticsResponse.DailyCountEntry.builder()
                .date(p.getDay())
                .count(p.getCount())
                .build();
    }

    private StatisticsResponse.AgentRejectionRate toAgentRejectionRate(AgentRejectionProjection p) {
        double rate = p.getTotalCount() > 0
                ? (p.getRejectedCount() * 100.0 / p.getTotalCount())
                : 0.0;
        return StatisticsResponse.AgentRejectionRate.builder()
                .agentId(p.getAgentId())
                .firstName(p.getFirstName())
                .lastName(p.getLastName())
                .totalCount(p.getTotalCount())
                .rejectedCount(p.getRejectedCount())
                .rejectionRatePercent(rate)
                .build();
    }
}