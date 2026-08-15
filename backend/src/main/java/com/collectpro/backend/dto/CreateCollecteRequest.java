package com.collectpro.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCollecteRequest {

    @NotNull(message = "La version du formulaire est obligatoire")
    private Long formVersionId;

    @NotBlank(message = "Les données de la collecte (dataJson) sont obligatoires")
    private String dataJson;

    private Double latitude;

    private Double longitude;
}