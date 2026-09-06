package com.collectpro.backend.repository;

import com.collectpro.backend.entity.Organization;
import com.collectpro.backend.enums.OrganizationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findByName(String name);

    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    long countByStatus(OrganizationStatus status);
}