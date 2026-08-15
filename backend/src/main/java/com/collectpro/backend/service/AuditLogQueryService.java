package com.collectpro.backend.service;

import com.collectpro.backend.dto.AuditLogResponse;
import com.collectpro.backend.entity.AuditLog;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.exception.ResourceNotFoundException;
import com.collectpro.backend.repository.AuditLogRepository;
import com.collectpro.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogQueryService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLogsForUser(User requester) {
        User managedRequester = userRepository.findById(requester.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        List<AuditLog> logs;
        if (managedRequester.getRole().getName() == RoleType.SUPER_ADMIN) {
            logs = auditLogRepository.findAllByOrderByCreatedAtDesc();
        } else {
            if (managedRequester.getOrganization() == null) {
                return List.of();
            }
            logs = auditLogRepository.findByOrganizationIdOrderByCreatedAtDesc(
                    managedRequester.getOrganization().getId());
        }

        return logs.stream().map(this::toResponse).toList();
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
