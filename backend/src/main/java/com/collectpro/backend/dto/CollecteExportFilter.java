package com.collectpro.backend.dto;

import com.collectpro.backend.enums.CollecteStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class CollecteExportFilter {
    private Long missionId;
    private Long formId;
    private CollecteStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String search;
}
