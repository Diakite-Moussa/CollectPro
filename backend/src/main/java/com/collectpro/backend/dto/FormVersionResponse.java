package com.collectpro.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class FormVersionResponse {
    private Long id;
    private Long formId;
    private Integer versionNumber;
    private String schemaJson;
    private LocalDateTime createdAt;
    private Long createdById;
}