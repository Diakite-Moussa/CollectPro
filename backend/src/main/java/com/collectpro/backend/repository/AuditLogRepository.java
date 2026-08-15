package com.collectpro.backend.repository;

import com.collectpro.backend.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId);

    List<AuditLog> findAllByOrderByCreatedAtDesc();
}
