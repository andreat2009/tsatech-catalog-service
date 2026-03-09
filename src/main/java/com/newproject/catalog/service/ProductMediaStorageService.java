package com.newproject.catalog.service;

import com.newproject.catalog.config.CatalogMediaProperties;
import com.newproject.catalog.exception.BadRequestException;
import com.newproject.catalog.exception.NotFoundException;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductMediaStorageService {

    private final CatalogMediaProperties properties;

    public ProductMediaStorageService(CatalogMediaProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void ensureStorageRoot() {
        try {
            Files.createDirectories(properties.rootPath());
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot create media storage root", ex);
        }
    }

    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is required");
        }

        long size = file.getSize();
        if (size > properties.getMaxFileSizeBytes()) {
            throw new BadRequestException("Image exceeds max allowed size");
        }

        String contentType = normalizeContentType(file.getContentType());
        Set<String> allowed = normalizedAllowedMimeTypes(properties.getAllowedMimeTypes());
        if (!allowed.contains(contentType)) {
            throw new BadRequestException("Unsupported image content type: " + contentType);
        }

        String extension = extensionFromContentType(contentType, file.getOriginalFilename());
        String storedFilename = UUID.randomUUID() + extension;
        Path target = resolveSafe(storedFilename);

        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot save image file", ex);
        }

        return new StoredFile(
            storedFilename,
            safeOriginalFilename(file.getOriginalFilename()),
            contentType,
            size
        );
    }

    public Resource loadAsResource(String storedFilename) {
        Path filePath = resolveSafe(storedFilename);
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new NotFoundException("Media file not found");
        }

        try {
            return new UrlResource(filePath.toUri());
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot read media file", ex);
        }
    }

    public MediaType detectMediaType(String storedFilename) {
        Path filePath = resolveSafe(storedFilename);
        try {
            String probed = Files.probeContentType(filePath);
            if (probed == null || probed.isBlank()) {
                return MediaType.APPLICATION_OCTET_STREAM;
            }
            return MediaType.parseMediaType(probed);
        } catch (Exception ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    public String publicUrl(String storedFilename) {
        return "/api/catalog/media/" + storedFilename;
    }

    public boolean isManagedMediaUrl(String url) {
        return url != null && url.startsWith("/api/catalog/media/");
    }

    public String extractStoredFilename(String url) {
        if (!isManagedMediaUrl(url)) {
            return null;
        }
        return url.substring("/api/catalog/media/".length());
    }

    public void deleteQuietly(String storedFilename) {
        if (storedFilename == null || storedFilename.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolveSafe(storedFilename));
        } catch (Exception ignored) {
            // Best-effort cleanup; metadata consistency remains source of truth.
        }
    }

    private Set<String> normalizedAllowedMimeTypes(List<String> configured) {
        return configured.stream()
            .map(this::normalizeContentType)
            .collect(Collectors.toSet());
    }

    private String normalizeContentType(String raw) {
        if (raw == null || raw.isBlank()) {
            return "application/octet-stream";
        }
        return raw.toLowerCase(Locale.ROOT).trim();
    }

    private String safeOriginalFilename(String original) {
        if (original == null || original.isBlank()) {
            return "upload";
        }
        return original
            .replace('\\', '_')
            .replace('/', '_')
            .replace("..", "_");
    }

    private String extensionFromContentType(String contentType, String originalFilename) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> extensionFromOriginalName(originalFilename);
        };
    }

    private String extensionFromOriginalName(String originalFilename) {
        if (originalFilename == null) {
            return ".bin";
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            return ".bin";
        }
        String ext = originalFilename.substring(dot).toLowerCase(Locale.ROOT);
        if (ext.length() > 10 || ext.contains("/") || ext.contains("\\")) {
            return ".bin";
        }
        return ext;
    }

    private Path resolveSafe(String storedFilename) {
        if (storedFilename == null || storedFilename.isBlank()) {
            throw new BadRequestException("Invalid media file name");
        }
        if (storedFilename.contains("/") || storedFilename.contains("\\") || storedFilename.contains("..")) {
            throw new BadRequestException("Invalid media file name");
        }

        Path root = properties.rootPath();
        Path resolved = root.resolve(storedFilename).normalize();
        if (!resolved.startsWith(root)) {
            throw new BadRequestException("Invalid media file path");
        }
        return resolved;
    }

    public record StoredFile(
        String storedFilename,
        String originalFilename,
        String contentType,
        long sizeBytes
    ) {}
}
