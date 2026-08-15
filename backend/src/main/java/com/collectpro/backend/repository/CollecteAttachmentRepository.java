package com.collectpro.backend.repository;

import com.collectpro.backend.entity.CollecteAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CollecteAttachmentRepository extends JpaRepository<CollecteAttachment, Long> {
    List<CollecteAttachment> findByCollecteId(Long collecteId);
    Optional<CollecteAttachment> findByStoredFilename(String storedFilename);
}
