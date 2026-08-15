package com.collectpro.backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PermissionResponse {
    private String code;
    private String description;
}
