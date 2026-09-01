package com.collectpro.backend.dto;

import com.collectpro.backend.enums.ReportType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReportResponse {
    private Long id;
    private Long organizationId;
    private Long missionId;
    private ReportType type;
    private String filePath;
    private GeneratedBySummary generatedBy;
    private LocalDateTime generatedAt;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    @Getter
    @Builder
    public static class GeneratedBySummary {
        private Long id;
        private String firstName;
        private String lastName;
    }
}