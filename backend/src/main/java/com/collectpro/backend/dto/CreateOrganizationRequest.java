package com.collectpro.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateOrganizationRequest {

    @NotBlank(message = "Le nom de l'organisation est obligatoire")
    private String name;

    private String description;

    @NotBlank(message = "Le prénom de l'administrateur principal est obligatoire")
    private String adminFirstName;

    @NotBlank(message = "Le nom de l'administrateur principal est obligatoire")
    private String adminLastName;

    @NotBlank(message = "L'email de l'administrateur principal est obligatoire")
    @Email(message = "L'email de l'administrateur principal est invalide")
    private String adminEmail;

    private String adminPhone;
}