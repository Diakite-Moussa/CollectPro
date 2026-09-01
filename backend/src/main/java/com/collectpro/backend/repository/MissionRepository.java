package com.collectpro.backend.repository;

import com.collectpro.backend.entity.Mission;
import com.collectpro.backend.entity.Organization;
import com.collectpro.backend.enums.MissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MissionRepository extends JpaRepository<Mission, Long> {
    List<Mission> findByOrganization(Organization organization);
    List<Mission> findByOrganizationAndStatus(Organization organization, MissionStatus status);

    long countByOrganization_Id(Long organizationId);
    long countByOrganization_IdAndStatus(Long organizationId, MissionStatus status);
}