package com.collectpro.backend.repository;

import com.collectpro.backend.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    Page<Report> findByOrganizationIdOrderByGeneratedAtDesc(Long organizationId, Pageable pageable);

    Page<Report> findByMissionIdOrderByGeneratedAtDesc(Long missionId, Pageable pageable);
}