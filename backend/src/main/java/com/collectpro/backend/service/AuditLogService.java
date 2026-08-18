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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Enregistre une operation sensible (RNF-008 / RNF-010). Best-effort :
     * une erreur ici ne doit jamais faire echouer l'action metier reelle.
     * Transaction dediee (REQUIRES_NEW) pour ne pas etre annulee si
     * l'action appelante rollback pour une autre raison.
     *
     * ATTENTION : si l'entite reference (ex: Organization) vient d'etre
     * creee DANS LA MEME transaction appelante et n'est pas encore validee,
     * cette methode echouera (violation de cle etrangere : la transaction
     * REQUIRES_NEW, totalement separee, ne voit pas les lignes non
     * commitees). Dans ce cas, utiliser logAfterCommit() a la place.
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

    /**
     * Variante de log() a utiliser lorsque l'entite referencee (ex:
     * Organization) vient d'etre creee dans la transaction en cours et
     * n'est pas encore validee en base. Differe l'ecriture reelle du log
     * via un callback afterCommit() : le log n'est ecrit qu'une fois la
     * transaction appelante definitivement validee, donc l'entite est
     * garantie visible (plus de risque de violation de cle etrangere).
     * Si aucune transaction n'est active (appel hors contexte
     * transactionnel), ecrit immediatement comme log().
     */
    public void logAfterCommit(User actor, Organization organization, AuditAction action,
                                String entityType, Long entityId, String details) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log(actor, organization, action, entityType, entityId, details);
                }
            });
        } else {
            log(actor, organization, action, entityType, entityId, details);
        }
    }
}
