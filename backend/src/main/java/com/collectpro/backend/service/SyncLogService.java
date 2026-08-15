package com.collectpro.backend.service;

import com.collectpro.backend.dto.CreateSyncLogRequest;
import com.collectpro.backend.dto.SyncLogResponse;
import com.collectpro.backend.entity.Collecte;
import com.collectpro.backend.entity.SyncLog;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.repository.CollecteRepository;
import com.collectpro.backend.repository.SyncLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SyncLogService {

    private final SyncLogRepository syncLogRepository;
    private final CollecteRepository collecteRepository;

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

    private SyncLogResponse toResponse(SyncLog syncLog) {
        return SyncLogResponse.builder()
                .id(syncLog.getId())
                .localReference(syncLog.getLocalReference())
                .collecteId(syncLog.getCollecte() != null ? syncLog.getCollecte().getId() : null)
                .result(syncLog.getResult())
                .errorMessage(syncLog.getErrorMessage())
                .createdAt(syncLog.getCreatedAt())
                .build();
    }
}
