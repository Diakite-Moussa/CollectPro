package com.collectpro.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateFormVersionRequest {

    @NotBlank(message = "Le schéma du formulaire (schemaJson) est obligatoire")
    private String schemaJson;
}