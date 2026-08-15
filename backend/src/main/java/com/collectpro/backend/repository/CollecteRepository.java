package com.collectpro.backend.repository;

import com.collectpro.backend.entity.Collecte;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.CollecteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CollecteRepository extends JpaRepository<Collecte, Long> {
    List<Collecte> findByAgent(User agent);
    List<Collecte> findByAgentIn(List<User> agents);
    List<Collecte> findByAgentInAndStatus(List<User> agents, CollecteStatus status);
    List<Collecte> findByAgent_OrganizationId(Long organizationId);

    long countByAgent_OrganizationId(Long organizationId);
    long countByAgent_OrganizationIdAndStatus(Long organizationId, CollecteStatus status);
    long countByStatus(CollecteStatus status);
    long countByAgentIn(List<User> agents);
    long countByAgentInAndStatus(List<User> agents, CollecteStatus status);
}