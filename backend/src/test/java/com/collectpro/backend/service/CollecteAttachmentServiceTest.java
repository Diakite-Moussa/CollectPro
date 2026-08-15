package com.collectpro.backend.service;

import com.collectpro.backend.dto.CollecteAttachmentResponse;
import com.collectpro.backend.entity.Collecte;
import com.collectpro.backend.entity.CollecteAttachment;
import com.collectpro.backend.repository.CollecteAttachmentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollecteAttachmentServiceTest {

    @Mock
    private CollecteAttachmentRepository attachmentRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private CollecteAttachmentService collecteAttachmentService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Collecte collecte;

    @BeforeEach
    void setUp() {
        collecte = Collecte.builder().id(7L).build();
        // ObjectMapper réel injecté manuellement car @InjectMocks ne sait pas
        // construire un ObjectMapper "mocké utile" ici — voir note ci-dessous.
    }

    @Test
    @DisplayName("storeUploadedFiles - Enregistre les fichiers valides")
    void storeUploadedFiles_ValidFiles_ReturnsAttachments() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "contenu".getBytes());

        FileStorageService.StoredFile stored =
                new FileStorageService.StoredFile("photo.jpg", "collectes/7/uuid.jpg", "image/jpeg", 7L);

        when(fileStorageService.saveMultipartFile(eq(7L), any(MultipartFile.class))).thenReturn(stored);
        when(attachmentRepository.save(any(CollecteAttachment.class))).thenAnswer(inv -> {
            CollecteAttachment a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

        List<CollecteAttachmentResponse> result =
                collecteAttachmentService.storeUploadedFiles(collecte, List.of(file));

        assertEquals(1, result.size());
        assertEquals("photo.jpg", result.get(0).getOriginalFilename());
        assertEquals("/files/attachments/1", result.get(0).getUrl());
    }

    @Test
    @DisplayName("storeUploadedFiles - Liste vide ou nulle retourne une liste vide sans appel au stockage")
    void storeUploadedFiles_EmptyOrNullList_ReturnsEmpty() {
        assertTrue(collecteAttachmentService.storeUploadedFiles(collecte, List.of()).isEmpty());
        assertTrue(collecteAttachmentService.storeUploadedFiles(collecte, null).isEmpty());
        verifyNoInteractions(fileStorageService, attachmentRepository);
    }

    @Test
    @DisplayName("storeUploadedFiles - Un fichier vide dans la liste est ignoré, pas planté")
    void storeUploadedFiles_EmptyFileInList_IsSkipped() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        List<CollecteAttachmentResponse> result =
                collecteAttachmentService.storeUploadedFiles(collecte, List.of(emptyFile));

        assertTrue(result.isEmpty());
        verifyNoInteractions(fileStorageService);
    }

    @Test
    @DisplayName("storeUploadedFiles - Une exception de stockage sur un fichier n'empêche pas les autres")
    void storeUploadedFiles_OneFails_OthersStillSaved() throws Exception {
        MockMultipartFile badFile = new MockMultipartFile("file", "bad.jpg", "image/jpeg", "x".getBytes());
        MockMultipartFile goodFile = new MockMultipartFile("file", "good.jpg", "image/jpeg", "y".getBytes());

        FileStorageService.StoredFile storedGood =
                new FileStorageService.StoredFile("good.jpg", "collectes/7/uuid2.jpg", "image/jpeg", 5L);

        when(fileStorageService.saveMultipartFile(eq(7L), argThat(f -> "bad.jpg".equals(f.getOriginalFilename()))))
                .thenThrow(new java.io.IOException("disque plein"));
        when(fileStorageService.saveMultipartFile(eq(7L), argThat(f -> "good.jpg".equals(f.getOriginalFilename()))))
                .thenReturn(storedGood);
        when(attachmentRepository.save(any(CollecteAttachment.class))).thenAnswer(inv -> {
            CollecteAttachment a = inv.getArgument(0);
            a.setId(2L);
            return a;
        });

        List<CollecteAttachmentResponse> result =
                collecteAttachmentService.storeUploadedFiles(collecte, List.of(badFile, goodFile));

        assertEquals(1, result.size());
        assertEquals("good.jpg", result.get(0).getOriginalFilename());
    }

    @Test
    @DisplayName("getAttachmentsForCollecte - Retourne les pièces jointes existantes")
    void getAttachmentsForCollecte_ReturnsList() {
        CollecteAttachment attachment = CollecteAttachment.builder()
                .id(1L).originalFilename("a.jpg").contentType("image/jpeg").sizeBytes(100L).build();

        when(attachmentRepository.findByCollecteId(7L)).thenReturn(List.of(attachment));

        List<CollecteAttachmentResponse> result = collecteAttachmentService.getAttachmentsForCollecte(7L);

        assertEquals(1, result.size());
        assertEquals("a.jpg", result.get(0).getOriginalFilename());
    }
}
