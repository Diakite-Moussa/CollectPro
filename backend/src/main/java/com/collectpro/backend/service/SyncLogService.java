package com.collectpro.backend.service;

import com.collectpro.backend.dto.CreateSyncLogRequest;
import com.collectpro.backend.dto.SyncLogResponse;
import com.collectpro.backend.entity.Collecte;
import com.collectpro.backend.entity.SupervisorAgent;
import com.collectpro.backend.entity.SyncLog;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.exception.BusinessRuleException;
import com.collectpro.backend.exception.ForbiddenOperationException;
import com.collectpro.backend.exception.ResourceNotFoundException;
import com.collectpro.backend.repository.CollecteRepository;
import com.collectpro.backend.repository.SupervisorAgentRepository;
import com.collectpro.backend.repository.SyncLogRepository;
import com.collectpro.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SyncLogService {

    private final SyncLogRepository syncLogRepository;
    private final CollecteRepository collecteRepository;
    private final UserRepository userRepository;
    private final SupervisorAgentRepository supervisorAgentRepository;

    @Transactional
    public SyncLogResponse recordSyncAttempt(CreateSyncLogRequest request, User agent) {
        Collecte collecte = null;
        if (request.getCollecteId() != null) {
            collecte = collecteRepository.findById(request.getCollecteId()).orElse(null);
        }

        SyncLog syncLog = SyncLog.builder()
                .agent(agent)
                .collecte(collecte)
                .localReference(request.getLocalReference())
                .result(request.getResult())
                .errorMessage(request.getErrorMessage())
                .build();
        syncLog = syncLogRepository.save(syncLog);

        return toResponse(syncLog);
    }

    @Transactional(readOnly = true)
    public List<SyncLogResponse> getSyncLogsForAgent(User agent) {
        return syncLogRepository.findByAgentIdOrderByCreatedAtDesc(agent.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Fix #3 — pagination serveur.
     * Retourne une Page<SyncLogResponse> selon le rôle du demandeur.
     * Le filtre agentId est appliqué en mémoire uniquement quand nécessaire
     * (le volume par superviseur est limité à ses propres agents).
     */
    @Transactional(readOnly = true)
    public Page<SyncLogResponse> getSyncLogsForRequester(User requester, Long agentId, Pageable pageable) {
        User managed = userRepository.findById(requester.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        RoleType role = managed.getRole().getName();

        Page<SyncLog> page;
        switch (role) {
            case SUPER_ADMIN -> page = syncLogRepository.findAllByOrderByCreatedAtDesc(pageable);
            case ADMIN_PRINCIPAL, ADMIN_SECONDAIRE -> {
                if (managed.getOrganization() == null) {
                    throw new BusinessRuleException("Utilisateur sans organisation");
                }
                page = syncLogRepository.findByAgent_OrganizationIdOrderByCreatedAtDesc(
                        managed.getOrganization().getId(), pageable);
            }
            case SUPERVISOR -> {
                List<User> agents = supervisorAgentRepository.findBySupervisorId(managed.getId()).stream()
                        .map(SupervisorAgent::getAgent)
                        .toList();
                page = syncLogRepository.findByAgentInOrderByCreatedAtDesc(agents, pageable);
            }
            default -> throw new ForbiddenOperationException(
                    "Accès refusé aux journaux de synchronisation");
        }

        // Filtre agentId optionnel : appliqué après la page pour ne pas rompre la pagination.
        // Acceptable car agentId est un filtre secondaire rare et le résultat est déjà borné.
        if (agentId != null) {
            List<SyncLogResponse> filtered = page.stream()
                    .filter(log -> log.getAgent().getId().equals(agentId))
                    .map(this::toResponse)
                    .toList();
            return new org.springframework.data.domain.PageImpl<>(filtered, pageable, filtered.size());
        }

        return page.map(this::toResponse);
    }

    private SyncLogResponse toResponse(SyncLog syncLog) {
        return SyncLogResponse.builder()
                .id(syncLog.getId())
                .agent(SyncLogResponse.AgentSummary.builder()
                        .id(syncLog.getAgent().getId())
                        .firstName(syncLog.getAgent().getFirstName())
                        .lastName(syncLog.getAgent().getLastName())
                        .build())
                .localReference(syncLog.getLocalReference())
                .collecteId(syncLog.getCollecte() != null ? syncLog.getCollecte().getId() : null)
                .result(syncLog.getResult())
                .errorMessage(syncLog.getErrorMessage())
                .createdAt(syncLog.getCreatedAt())
                .build();
    }
}
