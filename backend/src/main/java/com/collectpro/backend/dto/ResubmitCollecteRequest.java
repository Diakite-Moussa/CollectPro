package com.collectpro.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResubmitCollecteRequest {

    @NotBlank(message = "Les données de la collecte (dataJson) sont obligatoires")
    private String dataJson;

    private Double latitude;

    private Double longitude;
}