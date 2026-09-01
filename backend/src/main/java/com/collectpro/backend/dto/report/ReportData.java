package com.collectpro.backend.dto.report;

import com.collectpro.backend.enums.ReportType;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record ReportData(
        ReportType type,
        OrganizationReportInfo organization,
        MissionReportInfo mission,
        ReportPeriod period,
        ReportKpis kpis,
        List<AgentActivitySummary> agentsActivity,
        List<FormCollecteSummary> formsActivity,
        List<DailyTrendPoint> dailyTrend
) {

    @Builder
    public record OrganizationReportInfo(
            Long id,
            String name,
            String description,
            String logoUrl,
            String logoStoredFilename
    ) {}

    @Builder
    public record MissionReportInfo(
            Long id,
            String name,
            String description,
            String status,
            Integer expectedCollectes,
            Long assignedAgentsCount,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {}

    @Builder
    public record ReportPeriod(
            LocalDateTime start,
            LocalDateTime end
    ) {}

    @Builder
    public record ReportKpis(
            long totalCollectes,
            long validatedCollectes,
            long rejectedCollectes,
            long pendingCollectes,
            double validationRate,
            double rejectionRate,
            double avgValidationTimeHours
    ) {}

    @Builder
    public record AgentActivitySummary(
            Long agentId,
            String agentName,
            String agentEmail,
            long total,
            long validated,
            long rejected,
            long pending
    ) {}

    @Builder
    public record FormCollecteSummary(
            Long formId,
            String formName,
            long totalCollectes,
            long validatedCollectes,
            long rejectedCollectes
    ) {}

    @Builder
    public record DailyTrendPoint(
            LocalDate date,
            long collectesCount
    ) {}
}
