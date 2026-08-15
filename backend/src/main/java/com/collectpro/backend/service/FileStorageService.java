package com.collectpro.backend.service;

import com.collectpro.backend.exception.BusinessRuleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class FileStorageService {

    private static final Pattern BASE64_DATA_URI =
            Pattern.compile("^data:([\\w/+.-]+);base64,(.+)$", Pattern.DOTALL);

    private final Path uploadRoot;

    public FileStorageService(@Value("${app.storage.upload-dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadRoot);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de créer le dossier de stockage : " + uploadRoot, e);
        }
    }

    public StoredFile saveMultipartFile(Long collecteId, MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new BusinessRuleException("Fichier vide");
        }
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String storedName = buildStoredFilename(collecteId, originalName);
        Path target = resolvePath(storedName);
        Files.createDirectories(target.getParent());
        file.transferTo(target);
        return new StoredFile(originalName, storedName, file.getContentType(), file.getSize());
    }

    public StoredFile saveBase64DataUri(Long collecteId, String dataUri, String fallbackName) throws IOException {
        Matcher matcher = BASE64_DATA_URI.matcher(dataUri.trim());
        if (!matcher.matches()) {
            throw new BusinessRuleException("Format Base64 invalide pour le fichier");
        }
        String contentType = matcher.group(1);
        byte[] bytes = Base64.getDecoder().decode(matcher.group(2));
        String ext = extensionFromContentType(contentType);
        String originalName = fallbackName.contains(".") ? fallbackName : fallbackName + ext;
        String storedName = buildStoredFilename(collecteId, originalName);
        Path target = resolvePath(storedName);
        Files.createDirectories(target.getParent());
        Files.write(target, bytes);
        return new StoredFile(originalName, storedName, contentType, bytes.length);
    }

    public Path resolvePath(String storedFilename) {
        Path resolved = uploadRoot.resolve(storedFilename).normalize();
        if (!resolved.startsWith(uploadRoot)) {
            throw new BusinessRuleException("Chemin de fichier invalide");
        }
        return resolved;
    }

    private String buildStoredFilename(Long collecteId, String originalFilename) {
        String safeExt = "";
        int dot = originalFilename.lastIndexOf('.');
        if (dot > 0 && dot < originalFilename.length() - 1) {
            safeExt = originalFilename.substring(dot).replaceAll("[^a-zA-Z0-9.]", "");
        }
        return "collectes/" + collecteId + "/" + UUID.randomUUID() + safeExt;
    }

    private String extensionFromContentType(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "application/pdf" -> ".pdf";
            default -> ".bin";
        };
    }

    public record StoredFile(String originalFilename, String storedFilename, String contentType, long sizeBytes) {}
}
