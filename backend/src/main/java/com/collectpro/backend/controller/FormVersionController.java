package com.collectpro.backend.controller;

import com.collectpro.backend.dto.CreateFormVersionRequest;
import com.collectpro.backend.dto.FormVersionResponse;
import com.collectpro.backend.security.CustomUserDetails;
import com.collectpro.backend.service.FormVersionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/forms/{formId}/versions")
@RequiredArgsConstructor
public class FormVersionController {

    private final FormVersionService formVersionService;

    @PostMapping
    @PreAuthorize("@securityAuth.hasPermission(authentication, 'CREATE_FORM')")
    public ResponseEntity<FormVersionResponse> createVersion(
            @PathVariable Long formId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CreateFormVersionRequest request) {
        FormVersionResponse response = formVersionService.createVersion(formId, request, principal.getUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FormVersionResponse>> getVersions(
            @PathVariable Long formId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(formVersionService.getVersionsForForm(formId, principal.getUser()));
    }
}