package com.collectpro.backend.repository;

import com.collectpro.backend.entity.SupervisorAgent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SupervisorAgentRepository extends JpaRepository<SupervisorAgent, Long> {
    List<SupervisorAgent> findBySupervisorId(Long supervisorId);
    Optional<SupervisorAgent> findByAgentId(Long agentId);
    List<SupervisorAgent> findByAgentIdIn(Collection<Long> agentIds);
}