package com.collectpro.backend.repository;

import com.collectpro.backend.entity.Form;
import com.collectpro.backend.entity.FormVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FormVersionRepository extends JpaRepository<FormVersion, Long> {
    List<FormVersion> findByFormOrderByVersionNumberDesc(Form form);
    Optional<FormVersion> findFirstByFormOrderByVersionNumberDesc(Form form);
}