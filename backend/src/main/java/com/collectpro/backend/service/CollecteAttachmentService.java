package com.collectpro.backend.service;

import com.collectpro.backend.dto.CollecteAttachmentResponse;
import com.collectpro.backend.entity.Collecte;
import com.collectpro.backend.entity.CollecteAttachment;
import com.collectpro.backend.repository.CollecteAttachmentRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollecteAttachmentService {

    private final CollecteAttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    @Transactional
    public List<CollecteAttachmentResponse> storeUploadedFiles(Collecte collecte, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        List<CollecteAttachmentResponse> saved = new ArrayList<>();
        int index = 0;
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            try {
                saved.add(saveFile(collecte, file, "photo_" + (++index)));
            } catch (Exception e) {
                log.warn("Échec enregistrement pièce jointe collecte {} : {}", collecte.getId(), e.getMessage());
            }
        }
        return saved;
    }

    /**
     * Extrait les photos Base64 legacy (_photos dans dataJson) vers le stockage fichier.
     * Retourne le dataJson nettoyé (sans _photos).
     */
    @Transactional
    public String migrateLegacyBase64Photos(Collecte collecte, String dataJson) {
        try {
            Map<String, Object> payload = objectMapper.readValue(dataJson, new TypeReference<>() {});
            Object photosObj = payload.remove("_photos");
            if (!(photosObj instanceof List<?> photos) || photos.isEmpty()) {
                return dataJson;
            }
            int index = 0;
            for (Object photo : photos) {
                if (photo instanceof String dataUri) {
                    try {
                        saveBase64(collecte, dataUri, "legacy_photo_" + (++index));
                    } catch (Exception e) {
                        log.warn("Migration Base64 échouée pour collecte {} : {}", collecte.getId(), e.getMessage());
                    }
                }
            }
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("Impossible de parser dataJson pour migration photos : {}", e.getMessage());
            return dataJson;
        }
    }

    @Transactional(readOnly = true)
    public List<CollecteAttachmentResponse> getAttachmentsForCollecte(Long collecteId) {
        return attachmentRepository.findByCollecteId(collecteId).stream()
                .map(this::toResponse)
                .toList();
    }

    private CollecteAttachmentResponse saveFile(Collecte collecte, MultipartFile file, String fallbackName)
            throws Exception {
        FileStorageService.StoredFile stored = fileStorageService.saveMultipartFile(collecte.getId(), file);
        CollecteAttachment attachment = attachmentRepository.save(CollecteAttachment.builder()
                .collecte(collecte)
                .originalFilename(stored.originalFilename())
                .storedFilename(stored.storedFilename())
                .contentType(stored.contentType())
                .sizeBytes(stored.sizeBytes())
                .build());
        return toResponse(attachment);
    }

    private CollecteAttachmentResponse saveBase64(Collecte collecte, String dataUri, String fallbackName)
            throws Exception {
        FileStorageService.StoredFile stored =
                fileStorageService.saveBase64DataUri(collecte.getId(), dataUri, fallbackName);
        CollecteAttachment attachment = attachmentRepository.save(CollecteAttachment.builder()
                .collecte(collecte)
                .originalFilename(stored.originalFilename())
                .storedFilename(stored.storedFilename())
                .contentType(stored.contentType())
                .sizeBytes(stored.sizeBytes())
                .build());
        return toResponse(attachment);
    }

    private CollecteAttachmentResponse toResponse(CollecteAttachment attachment) {
        return CollecteAttachmentResponse.builder()
                .id(attachment.getId())
                .originalFilename(attachment.getOriginalFilename())
                .contentType(attachment.getContentType())
                .sizeBytes(attachment.getSizeBytes())
                .url("/files/attachments/" + attachment.getId())
                .build();
    }
}
