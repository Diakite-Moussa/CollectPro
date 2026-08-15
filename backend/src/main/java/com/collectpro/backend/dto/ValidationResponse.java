package com.collectpro.backend.dto;

import com.collectpro.backend.enums.ValidationDecision;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ValidationResponse {
    private Long id;
    private ValidationDecision decision;
    private String comment;
    private LocalDateTime createdAt;
    private SupervisorSummary supervisor;

    @Getter
    @Builder
    public static class SupervisorSummary {
        private Long id;
        private String firstName;
        private String lastName;
    }
}