package com.collectpro.backend.repository;

import com.collectpro.backend.entity.SyncLog;
import com.collectpro.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SyncLogRepository extends JpaRepository<SyncLog, Long> {

    // ---- Sans pagination (utilisé par l'agent mobile pour ses propres logs) ----
    List<SyncLog> findByAgentIdOrderByCreatedAtDesc(Long agentId);

    // ---- Avec pagination (Fix #3) ----

    /** Tous les sync-logs — périmètre SUPER_ADMIN. */
    Page<SyncLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Sync-logs de l'organisation — périmètre Admin. */
    Page<SyncLog> findByAgent_OrganizationIdOrderByCreatedAtDesc(Long organizationId, Pageable pageable);

    /** Sync-logs des agents supervisés — périmètre Superviseur. */
    Page<SyncLog> findByAgentInOrderByCreatedAtDesc(List<User> agents, Pageable pageable);

    // ---- Variantes filtrées par agentId (appliquées en base, pas en mémoire) ----

    /** SUPER_ADMIN + filtre agentId. */
    Page<SyncLog> findByAgent_IdOrderByCreatedAtDesc(Long agentId, Pageable pageable);

    /** Admin + filtre agentId (garantit que l'agent appartient bien à l'organisation). */
    Page<SyncLog> findByAgent_OrganizationIdAndAgent_IdOrderByCreatedAtDesc(
            Long organizationId, Long agentId, Pageable pageable);
}
