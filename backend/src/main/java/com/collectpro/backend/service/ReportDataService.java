package com.collectpro.backend.service;

import com.collectpro.backend.dto.report.ReportData;
import com.collectpro.backend.entity.Collecte;
import com.collectpro.backend.entity.Form;
import com.collectpro.backend.entity.Mission;
import com.collectpro.backend.entity.Organization;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.CollecteStatus;
import com.collectpro.backend.enums.ReportType;
import com.collectpro.backend.exception.ResourceNotFoundException;
import com.collectpro.backend.repository.CollecteRepository;
import com.collectpro.backend.repository.MissionAgentRepository;
import com.collectpro.backend.repository.MissionRepository;
import com.collectpro.backend.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportDataService {

    private final OrganizationRepository organizationRepository;
    private final MissionRepository missionRepository;
    private final MissionAgentRepository missionAgentRepository;
    private final CollecteRepository collecteRepository;

    public ReportData collectOrganizationReportData(Long organizationId, LocalDateTime start, LocalDateTime end) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation introuvable : ID " + organizationId));

        List<Collecte> collectes = (start != null && end != null)
                ? collecteRepository.findByAgent_OrganizationIdAndCreatedAtBetween(organizationId, start, end)
                : collecteRepository.findByAgent_OrganizationId(organizationId);

        ReportData.OrganizationReportInfo orgInfo = ReportData.OrganizationReportInfo.builder()
                .id(organization.getId())
                .name(organization.getName())
                .description(organization.getDescription())
                .logoUrl(organization.getLogoUrl() != null ? "/files/organizations/" + organization.getId() + "/logo" : null)
                .logoStoredFilename(organization.getLogoUrl())
                .build();

        return buildReportData(
                ReportType.ORGANIZATION,
                orgInfo,
                null,
                start,
                end,
                collectes
        );
    }

    public ReportData collectMissionReportData(Long missionId, LocalDateTime start, LocalDateTime end) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission introuvable : ID " + missionId));

        Organization organization = mission.getOrganization();
        long assignedAgentsCount = missionAgentRepository.countByMissionId(missionId);

        List<Collecte> collectes = (start != null && end != null)
                ? collecteRepository.findByMissionIdAndCreatedAtBetween(missionId, start, end)
                : collecteRepository.findByMissionId(missionId);

        ReportData.OrganizationReportInfo orgInfo = ReportData.OrganizationReportInfo.builder()
                .id(organization.getId())
                .name(organization.getName())
                .description(organization.getDescription())
                .logoUrl(organization.getLogoUrl() != null ? "/files/organizations/" + organization.getId() + "/logo" : null)
                .logoStoredFilename(organization.getLogoUrl())
                .build();

        ReportData.MissionReportInfo missionInfo = ReportData.MissionReportInfo.builder()
                .id(mission.getId())
                .name(mission.getName())
                .description(mission.getDescription())
                .status(mission.getStatus() != null ? mission.getStatus().name() : null)
                .expectedCollectes(mission.getExpectedCollectesCount())
                .assignedAgentsCount(assignedAgentsCount)
                .startDate(mission.getStartDate())
                .endDate(mission.getEndDate())
                .build();

        return buildReportData(
                ReportType.MISSION,
                orgInfo,
                missionInfo,
                start,
                end,
                collectes
        );
    }

    private ReportData buildReportData(
            ReportType type,
            ReportData.OrganizationReportInfo orgInfo,
            ReportData.MissionReportInfo missionInfo,
            LocalDateTime start,
            LocalDateTime end,
            List<Collecte> collectes
    ) {
        long total = collectes.size();
        long validated = collectes.stream().filter(c -> c.getStatus() == CollecteStatus.VALIDATED).count();
        long rejected = collectes.stream().filter(c -> c.getStatus() == CollecteStatus.REJECTED).count();
        long pending = collectes.stream().filter(c -> c.getStatus() == CollecteStatus.PENDING_VALIDATION).count();

        double validationRate = total > 0 ? (validated * 100.0 / total) : 0.0;
        double rejectionRate = total > 0 ? (rejected * 100.0 / total) : 0.0;

        List<Double> validationDurationsHours = collectes.stream()
                .filter(c -> c.getStatus() == CollecteStatus.VALIDATED && c.getValidatedAt() != null && c.getCreatedAt() != null)
                .map(c -> Duration.between(c.getCreatedAt(), c.getValidatedAt()).toSeconds() / 3600.0)
                .filter(d -> d >= 0)
                .toList();

        double avgValidationTimeHours = validationDurationsHours.isEmpty()
                ? 0.0
                : validationDurationsHours.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        ReportData.ReportKpis kpis = ReportData.ReportKpis.builder()
                .totalCollectes(total)
                .validatedCollectes(validated)
                .rejectedCollectes(rejected)
                .pendingCollectes(pending)
                .validationRate(Math.round(validationRate * 10.0) / 10.0)
                .rejectionRate(Math.round(rejectionRate * 10.0) / 10.0)
                .avgValidationTimeHours(Math.round(avgValidationTimeHours * 10.0) / 10.0)
                .build();

        // Agents Activity Summary
        Map<User, List<Collecte>> byAgent = collectes.stream()
                .filter(c -> c.getAgent() != null)
                .collect(Collectors.groupingBy(Collecte::getAgent));

        List<ReportData.AgentActivitySummary> agentsActivity = byAgent.entrySet().stream()
                .map(entry -> {
                    User agent = entry.getKey();
                    List<Collecte> agentCollectes = entry.getValue();
                    long agentTotal = agentCollectes.size();
                    long agentValidated = agentCollectes.stream().filter(c -> c.getStatus() == CollecteStatus.VALIDATED).count();
                    long agentRejected = agentCollectes.stream().filter(c -> c.getStatus() == CollecteStatus.REJECTED).count();
                    long agentPending = agentCollectes.stream().filter(c -> c.getStatus() == CollecteStatus.PENDING_VALIDATION).count();

                    return ReportData.AgentActivitySummary.builder()
                            .agentId(agent.getId())
                            .agentName(agent.getFirstName() + " " + agent.getLastName())
                            .agentEmail(agent.getEmail())
                            .total(agentTotal)
                            .validated(agentValidated)
                            .rejected(agentRejected)
                            .pending(agentPending)
                            .build();
                })
                .sorted(Comparator.comparingLong(ReportData.AgentActivitySummary::total).reversed())
                .toList();

        // Forms Activity Summary
        Map<Form, List<Collecte>> byForm = collectes.stream()
                .filter(c -> c.getFormVersion() != null && c.getFormVersion().getForm() != null)
                .collect(Collectors.groupingBy(c -> c.getFormVersion().getForm()));

        List<ReportData.FormCollecteSummary> formsActivity = byForm.entrySet().stream()
                .map(entry -> {
                    Form form = entry.getKey();
                    List<Collecte> formCollectes = entry.getValue();
                    long formTotal = formCollectes.size();
                    long formValidated = formCollectes.stream().filter(c -> c.getStatus() == CollecteStatus.VALIDATED).count();
                    long formRejected = formCollectes.stream().filter(c -> c.getStatus() == CollecteStatus.REJECTED).count();

                    return ReportData.FormCollecteSummary.builder()
                            .formId(form.getId())
                            .formName(form.getName())
                            .totalCollectes(formTotal)
                            .validatedCollectes(formValidated)
                            .rejectedCollectes(formRejected)
                            .build();
                })
                .sorted(Comparator.comparingLong(ReportData.FormCollecteSummary::totalCollectes).reversed())
                .toList();

        // Daily Trend
        Map<LocalDate, Long> byDay = collectes.stream()
                .filter(c -> c.getCreatedAt() != null)
                .collect(Collectors.groupingBy(c -> c.getCreatedAt().toLocalDate(), Collectors.counting()));

        List<ReportData.DailyTrendPoint> dailyTrend = byDay.entrySet().stream()
                .map(e -> ReportData.DailyTrendPoint.builder()
                        .date(e.getKey())
                        .collectesCount(e.getValue())
                        .build())
                .sorted(Comparator.comparing(ReportData.DailyTrendPoint::date))
                .toList();

        return ReportData.builder()
                .type(type)
                .organization(orgInfo)
                .mission(missionInfo)
                .period(ReportData.ReportPeriod.builder().start(start).end(end).build())
                .kpis(kpis)
                .agentsActivity(agentsActivity)
                .formsActivity(formsActivity)
                .dailyTrend(dailyTrend)
                .build();
    }
}
