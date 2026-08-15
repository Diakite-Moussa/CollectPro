package com.collectpro.backend.service;

import com.collectpro.backend.entity.AuditLog;
import com.collectpro.backend.entity.Organization;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.AuditAction;
import com.collectpro.backend.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Enregistre une opération sensible (RNF-008 / RNF-010). Best-effort :
     * une erreur ici ne doit jamais faire échouer l'action métier réelle.
     * Transaction dédiée (REQUIRES_NEW) pour ne pas être annulée si
     * l'action appelante rollback pour une autre raison.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(User actor, Organization organization, AuditAction action,
                     String entityType, Long entityId, String details) {
        try {
            AuditLog entry = AuditLog.builder()
                    .user(actor)
                    .organization(organization)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(details)
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.warn("Échec de l'écriture du journal d'audit ({}) : {}", action, e.getMessage());
        }
    }
}
