package com.collectpro.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateFormRequest {

    @NotBlank(message = "Le nom du formulaire est obligatoire")
    private String name;

    private String description;
}