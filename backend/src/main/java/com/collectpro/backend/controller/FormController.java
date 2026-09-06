package com.collectpro.backend.controller;

import com.collectpro.backend.dto.CreateFormRequest;
import com.collectpro.backend.dto.FormResponse;
import com.collectpro.backend.security.CustomUserDetails;
import com.collectpro.backend.service.FormService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/forms")
@RequiredArgsConstructor
public class FormController {

    private final FormService formService;

    @PostMapping
    @PreAuthorize("@securityAuth.hasPermission(authentication, 'CREATE_FORM')")
    public ResponseEntity<FormResponse> createForm(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CreateFormRequest request) {
        FormResponse response = formService.createForm(request, principal.getUser().getOrganization());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FormResponse>> getForms(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(formService.getFormsForOrganization(principal.getUser().getOrganization()));
    }

    @GetMapping("/published")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FormResponse>> getPublishedForms(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(formService.getPublishedFormsForOrganization(principal.getUser().getOrganization()));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("@securityAuth.hasPermission(authentication, 'PUBLISH_FORM')")
    public ResponseEntity<FormResponse> publishForm(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(formService.publishForm(principal.getUser(), id));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("@securityAuth.hasPermission(authentication, 'PUBLISH_FORM')")
    public ResponseEntity<FormResponse> archiveForm(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(formService.archiveForm(principal.getUser(), id));
    }
}