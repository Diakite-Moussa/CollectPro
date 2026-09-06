package com.collectpro.backend.service;

import com.collectpro.backend.dto.AuditLogResponse;
import com.collectpro.backend.entity.AuditLog;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.exception.ResourceNotFoundException;
import com.collectpro.backend.repository.AuditLogRepository;
import com.collectpro.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogQueryService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    /**
     * Fix #12 — pagination serveur.
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogsForUser(User requester, Pageable pageable) {
        User managedRequester = userRepository.findById(requester.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        Page<AuditLog> page;
        if (managedRequester.getRole().getName() == RoleType.SUPER_ADMIN) {
            page = auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        } else {
            if (managedRequester.getOrganization() == null) {
                return Page.empty(pageable);
            }
            page = auditLogRepository.findByOrganizationIdOrderByCreatedAtDesc(
                    managedRequester.getOrganization().getId(), pageable);
        }

        return page.map(this::toResponse);
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .details(log.getDetails())
                .createdAt(log.getCreatedAt())
                .actor(log.getUser() != null
                        ? AuditLogResponse.ActorSummary.builder()
                        .id(log.getUser().getId())
                        .firstName(log.getUser().getFirstName())
                        .lastName(log.getUser().getLastName())
                        .email(log.getUser().getEmail())
                        .build()
                        : null)
                .build();
    }
}