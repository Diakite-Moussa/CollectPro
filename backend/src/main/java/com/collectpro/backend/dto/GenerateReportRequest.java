package com.collectpro.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class GenerateReportRequest {
    // Optionnels : si absents, agrégation sur toute la période disponible.
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
}