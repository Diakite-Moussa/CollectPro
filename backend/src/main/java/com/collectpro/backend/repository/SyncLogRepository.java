package com.collectpro.backend.repository;

import com.collectpro.backend.entity.SyncLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SyncLogRepository extends JpaRepository<SyncLog, Long> {
    List<SyncLog> findByAgentIdOrderByCreatedAtDesc(Long agentId);
}
