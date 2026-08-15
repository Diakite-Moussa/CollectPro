package com.collectpro.backend.repository;

import com.collectpro.backend.entity.Validation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ValidationRepository extends JpaRepository<Validation, Long> {
    List<Validation> findByCollecteIdOrderByCreatedAtDesc(Long collecteId);
}