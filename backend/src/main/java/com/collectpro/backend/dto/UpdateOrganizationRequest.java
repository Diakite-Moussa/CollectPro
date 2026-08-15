package com.collectpro.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateOrganizationRequest {

    @NotBlank(message = "Le nom est obligatoire")
    private String name;

    private String description;
}