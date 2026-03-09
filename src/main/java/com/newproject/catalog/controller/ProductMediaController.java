package com.newproject.catalog.controller;

import com.newproject.catalog.service.ProductMediaStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/catalog/media")
public class ProductMediaController {

    private final ProductMediaStorageService mediaStorageService;

    public ProductMediaController(ProductMediaStorageService mediaStorageService) {
        this.mediaStorageService = mediaStorageService;
    }

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> getMedia(@PathVariable String filename) {
        Resource resource = mediaStorageService.loadAsResource(filename);
        MediaType mediaType = mediaStorageService.detectMediaType(filename);

        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
            .contentType(mediaType)
            .body(resource);
    }
}
