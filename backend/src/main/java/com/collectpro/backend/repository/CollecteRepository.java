package com.collectpro.backend.repository;

import com.collectpro.backend.entity.Collecte;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.CollecteStatus;
import com.collectpro.backend.repository.projection.AgentRejectionProjection;
import com.collectpro.backend.repository.projection.DailyCountProjection;
import com.collectpro.backend.repository.projection.MissionProgressProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CollecteRepository extends JpaRepository<Collecte, Long> {
    List<Collecte> findByAgent(User agent);
    List<Collecte> findByAgentIn(List<User> agents);
    List<Collecte> findByAgentInAndStatus(List<User> agents, CollecteStatus status);
    List<Collecte> findByAgent_OrganizationId(Long organizationId);
    List<Collecte> findByAgent_OrganizationIdAndCreatedAtBetween(Long organizationId, LocalDateTime start, LocalDateTime end);
    List<Collecte> findByAgentInAndMissionId(List<User> agents, Long missionId);
    List<Collecte> findByAgent_OrganizationIdAndMissionId(Long organizationId, Long missionId);
    List<Collecte> findByMissionId(Long missionId);
    List<Collecte> findByMissionIdAndCreatedAtBetween(Long missionId, LocalDateTime start, LocalDateTime end);

    long countByAgent_OrganizationId(Long organizationId);
    long countByAgent_OrganizationIdAndStatus(Long organizationId, CollecteStatus status);
    long countByStatus(CollecteStatus status);
    long countByAgentIn(List<User> agents);
    long countByAgentInAndStatus(List<User> agents, CollecteStatus status);

    // ==========================================================
    // Sprint 4 — Statistiques avancées
    // ==========================================================

    // ---- 1. Évolution des collectes par jour ----

    @Query("""
            SELECT CAST(c.createdAt AS localdate) AS day, COUNT(c) AS count
            FROM Collecte c
            WHERE c.createdAt >= :since
            GROUP BY CAST(c.createdAt AS localdate)
            ORDER BY day ASC
            """)
    List<DailyCountProjection> findDailyCountsGlobal(@Param("since") LocalDateTime since);

    @Query("""
            SELECT CAST(c.createdAt AS localdate) AS day, COUNT(c) AS count
            FROM Collecte c
            WHERE c.agent.organization.id = :organizationId
              AND c.createdAt >= :since
            GROUP BY CAST(c.createdAt AS localdate)
            ORDER BY day ASC
            """)
    List<DailyCountProjection> findDailyCountsByOrganization(
            @Param("organizationId") Long organizationId, @Param("since") LocalDateTime since);

    @Query("""
            SELECT CAST(c.createdAt AS localdate) AS day, COUNT(c) AS count
            FROM Collecte c
            WHERE c.agent IN :agents
              AND c.createdAt >= :since
            GROUP BY CAST(c.createdAt AS localdate)
            ORDER BY day ASC
            """)
    List<DailyCountProjection> findDailyCountsByAgents(
            @Param("agents") List<User> agents, @Param("since") LocalDateTime since);

    // ---- 2. Taux de rejet par agent ----

    @Query("""
            SELECT c.agent.id AS agentId, c.agent.firstName AS firstName, c.agent.lastName AS lastName,
                   COUNT(c) AS totalCount,
                   SUM(CASE WHEN c.status = com.collectpro.backend.enums.CollecteStatus.REJECTED THEN 1 ELSE 0 END) AS rejectedCount
            FROM Collecte c
            WHERE c.agent.organization.id = :organizationId
            GROUP BY c.agent.id, c.agent.firstName, c.agent.lastName
            """)
    List<AgentRejectionProjection> findRejectionRatesByOrganization(@Param("organizationId") Long organizationId);

    @Query("""
            SELECT c.agent.id AS agentId, c.agent.firstName AS firstName, c.agent.lastName AS lastName,
                   COUNT(c) AS totalCount,
                   SUM(CASE WHEN c.status = com.collectpro.backend.enums.CollecteStatus.REJECTED THEN 1 ELSE 0 END) AS rejectedCount
            FROM Collecte c
            WHERE c.agent IN :agents
            GROUP BY c.agent.id, c.agent.firstName, c.agent.lastName
            """)
    List<AgentRejectionProjection> findRejectionRatesByAgents(@Param("agents") List<User> agents);

    @Query("""
            SELECT c.agent.id AS agentId, c.agent.firstName AS firstName, c.agent.lastName AS lastName,
                   COUNT(c) AS totalCount,
                   SUM(CASE WHEN c.status = com.collectpro.backend.enums.CollecteStatus.REJECTED THEN 1 ELSE 0 END) AS rejectedCount
            FROM Collecte c
            GROUP BY c.agent.id, c.agent.firstName, c.agent.lastName
            """)
    List<AgentRejectionProjection> findRejectionRatesGlobal();

    // ---- 3. Temps moyen de validation (en heures) — SQL natif requis ----
    // JPQL ne supporte pas la syntaxe EXTRACT(EPOCH FROM ...) à l'intérieur
    // de FUNCTION(), on passe donc par du SQL natif PostgreSQL.
    // On exclut les collectes non encore validées (validated_at IS NULL)
    // pour ne pas biaiser la moyenne avec des durées inexistantes.

    @Query(value = """
            SELECT AVG(EXTRACT(EPOCH FROM (c.validated_at - c.created_at)) / 3600.0)
            FROM collectes c
            WHERE c.validated_at IS NOT NULL
            """, nativeQuery = true)
    Double findAvgValidationTimeHoursGlobal();

    @Query(value = """
            SELECT AVG(EXTRACT(EPOCH FROM (c.validated_at - c.created_at)) / 3600.0)
            FROM collectes c
            JOIN users u ON u.id = c.agent_id
            WHERE c.validated_at IS NOT NULL
              AND u.organization_id = :organizationId
            """, nativeQuery = true)
    Double findAvgValidationTimeHoursByOrganization(@Param("organizationId") Long organizationId);

    @Query(value = """
            SELECT AVG(EXTRACT(EPOCH FROM (c.validated_at - c.created_at)) / 3600.0)
            FROM collectes c
            WHERE c.validated_at IS NOT NULL
              AND c.agent_id IN :agentIds
            """, nativeQuery = true)
    Double findAvgValidationTimeHoursByAgentIds(@Param("agentIds") List<Long> agentIds);

    // ---- 4. Progression par mission (reçues + agents actifs) ----
    // Une seule requête groupée par mission_id : évite le pattern N+1
    // (pas de boucle Java qui interroge la BDD mission par mission).

    @Query("""
            SELECT c.mission.id AS missionId,
                   COUNT(c) AS receivedCount,
                   COUNT(DISTINCT c.agent.id) AS activeAgentsCount
            FROM Collecte c
            WHERE c.mission.id IN :missionIds
            GROUP BY c.mission.id
            """)
    List<MissionProgressProjection> findProgressByMissionIds(@Param("missionIds") List<Long> missionIds);
}