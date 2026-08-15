package com.collectpro.backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ActivationTokenStatusResponse {
    private boolean valid;
    private String email;
    private String firstName;
    private String lastName;
}