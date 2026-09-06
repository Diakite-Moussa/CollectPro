package com.collectpro.backend.repository;

import com.collectpro.backend.entity.Mission;
import com.collectpro.backend.entity.Organization;
import com.collectpro.backend.enums.MissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MissionRepository extends JpaRepository<Mission, Long> {
    List<Mission> findByOrganization(Organization organization);
    List<Mission> findByOrganizationAndStatus(Organization organization, MissionStatus status);

    // Fix #12 — pagination serveur (nouvel endpoint dashboard uniquement,
    // GET /missions reste inchangé — contrat consommé par l'app mobile)
    Page<Mission> findByOrganization(Organization organization, Pageable pageable);
    // Page<Mission> findAll(Pageable pageable) est déjà héritée de JpaRepository

    long countByOrganization_Id(Long organizationId);
    long countByOrganization_IdAndStatus(Long organizationId, MissionStatus status);
}