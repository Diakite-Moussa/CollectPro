package com.collectpro.backend.controller;

import com.collectpro.backend.repository.CollecteAttachmentRepository;
import com.collectpro.backend.security.CustomUserDetails;
import com.collectpro.backend.service.CollecteService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.collectpro.backend.service.OrganizationService;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final CollecteService collecteService;
    private final CollecteAttachmentRepository attachmentRepository;
    private final OrganizationService organizationService;

    @GetMapping("/attachments/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        var attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> new com.collectpro.backend.exception.ResourceNotFoundException(
                        "Pièce jointe introuvable"));
        Resource resource = collecteService.loadAttachmentResource(id, principal.getUser());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + attachment.getOriginalFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/organizations/{id}/logo")
    public ResponseEntity<Resource> downloadOrganizationLogo(@PathVariable Long id) {
        OrganizationService.LogoFile logo = organizationService.loadLogoFile(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(logo.contentType()))
                .body(logo.resource());
    }
}
