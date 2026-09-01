package com.collectpro.backend.repository;

import com.collectpro.backend.entity.MissionAgent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MissionAgentRepository extends JpaRepository<MissionAgent, Long> {
    List<MissionAgent> findByMissionId(Long missionId);
    List<MissionAgent> findByAgentId(Long agentId);
    List<MissionAgent> findByAgentIdIn(Collection<Long> agentIds);
    Optional<MissionAgent> findByMissionIdAndAgentId(Long missionId, Long agentId);
    boolean existsByMissionIdAndAgentId(Long missionId, Long agentId);
    void deleteByMissionIdAndAgentId(Long missionId, Long agentId);

    long countByMissionId(Long missionId);

    @Query("""
            SELECT ma.mission.id AS missionId, COUNT(ma) AS count
            FROM MissionAgent ma
            WHERE ma.mission.id IN :missionIds
            GROUP BY ma.mission.id
            """)
    List<com.collectpro.backend.repository.projection.MissionAgentCountProjection> countAssignedAgentsByMissionIds(
            @Param("missionIds") List<Long> missionIds);
}