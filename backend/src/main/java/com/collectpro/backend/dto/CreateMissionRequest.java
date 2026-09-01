package com.collectpro.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CreateMissionRequest {

    @NotBlank(message = "Le nom de la mission est obligatoire")
    private String name;

    private String description;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private Double latitude;

    private Double longitude;

    private Double radiusMeters;

    @Positive(message = "Le nombre de collectes attendues doit être positif")
    private Integer expectedCollectesCount;
}