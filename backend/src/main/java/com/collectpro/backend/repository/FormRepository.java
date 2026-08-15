package com.collectpro.backend.repository;

import com.collectpro.backend.entity.Form;
import com.collectpro.backend.entity.Organization;
import com.collectpro.backend.enums.FormStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FormRepository extends JpaRepository<Form, Long> {
    List<Form> findByOrganization(Organization organization);
    List<Form> findByOrganizationAndStatus(Organization organization, FormStatus status);

    long countByOrganization_Id(Long organizationId);
    long countByOrganization_IdAndStatus(Long organizationId, FormStatus status);
    long countByStatus(FormStatus status);
}