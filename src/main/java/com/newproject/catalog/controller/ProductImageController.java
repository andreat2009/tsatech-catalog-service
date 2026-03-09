package com.newproject.catalog.controller;

import com.newproject.catalog.dto.ProductImagesResponse;
import com.newproject.catalog.service.ProductImageService;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/catalog/products/{productId}/images")
public class ProductImageController {

    private final ProductImageService productImageService;

    public ProductImageController(ProductImageService productImageService) {
        this.productImageService = productImageService;
    }

    @GetMapping
    public ProductImagesResponse list(@PathVariable Long productId) {
        return productImageService.listByProduct(productId);
    }

    @PostMapping(path = "/cover", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductImagesResponse uploadCover(
        @PathVariable Long productId,
        @RequestPart("file") MultipartFile file
    ) {
        return productImageService.uploadCover(productId, file);
    }

    @PostMapping(path = "/gallery", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductImagesResponse uploadGallery(
        @PathVariable Long productId,
        @RequestPart("files") MultipartFile[] files
    ) {
        List<MultipartFile> fileList = files != null ? Arrays.asList(files) : List.of();
        return productImageService.uploadGallery(productId, fileList);
    }

    @PatchMapping("/{imageId}/cover")
    public ProductImagesResponse setCover(
        @PathVariable Long productId,
        @PathVariable Long imageId
    ) {
        return productImageService.setCover(productId, imageId);
    }

    @DeleteMapping("/{imageId}")
    public ProductImagesResponse delete(
        @PathVariable Long productId,
        @PathVariable Long imageId
    ) {
        return productImageService.deleteImage(productId, imageId);
    }
}
