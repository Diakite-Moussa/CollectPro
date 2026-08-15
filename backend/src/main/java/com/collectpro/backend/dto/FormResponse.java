package com.collectpro.backend.dto;

import com.collectpro.backend.enums.FormStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class FormResponse {
    private Long id;
    private String name;
    private String description;
    private FormStatus status;
    private LocalDateTime createdAt;
    private Long organizationId;
    private Integer latestVersionNumber;
}