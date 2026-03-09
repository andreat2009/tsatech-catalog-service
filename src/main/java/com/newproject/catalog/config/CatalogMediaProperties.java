package com.newproject.catalog.config;

import jakarta.annotation.PostConstruct;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "catalog.media")
public class CatalogMediaProperties {

    private String rootPath = "./data/catalog-media";
    private long maxFileSizeBytes = 5242880L;
    private List<String> allowedMimeTypes = List.of("image/jpeg", "image/png", "image/webp", "image/gif");

    @PostConstruct
    void normalizeValues() {
        if (maxFileSizeBytes <= 0) {
            maxFileSizeBytes = 5242880L;
        }
        if (allowedMimeTypes == null || allowedMimeTypes.isEmpty()) {
            allowedMimeTypes = List.of("image/jpeg", "image/png", "image/webp", "image/gif");
        }
    }

    public Path rootPath() {
        return Paths.get(rootPath).toAbsolutePath().normalize();
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public List<String> getAllowedMimeTypes() {
        return allowedMimeTypes;
    }

    public void setAllowedMimeTypes(List<String> allowedMimeTypes) {
        this.allowedMimeTypes = allowedMimeTypes;
    }
}
