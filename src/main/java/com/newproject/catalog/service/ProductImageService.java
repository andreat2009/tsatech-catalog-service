package com.newproject.catalog.service;

import com.newproject.catalog.domain.Product;
import com.newproject.catalog.domain.ProductImage;
import com.newproject.catalog.dto.ProductImageResponse;
import com.newproject.catalog.dto.ProductImagesResponse;
import com.newproject.catalog.exception.NotFoundException;
import com.newproject.catalog.repository.ProductImageRepository;
import com.newproject.catalog.repository.ProductRepository;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductImageService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductMediaStorageService mediaStorageService;

    public ProductImageService(
        ProductRepository productRepository,
        ProductImageRepository productImageRepository,
        ProductMediaStorageService mediaStorageService
    ) {
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.mediaStorageService = mediaStorageService;
    }

    @Transactional(readOnly = true)
    public ProductImagesResponse listByProduct(Long productId) {
        Product product = requireProduct(productId);
        return toImagesResponse(product);
    }

    @Transactional
    public ProductImagesResponse uploadCover(Long productId, MultipartFile file) {
        Product product = requireProduct(productId);
        ProductMediaStorageService.StoredFile stored = mediaStorageService.store(file);

        productImageRepository.findFirstByProductIdAndCoverTrue(productId).ifPresent(existingCover -> {
            existingCover.setCover(false);
            productImageRepository.save(existingCover);
        });

        ProductImage coverImage = new ProductImage();
        coverImage.setProduct(product);
        coverImage.setStoredFilename(stored.storedFilename());
        coverImage.setOriginalFilename(stored.originalFilename());
        coverImage.setContentType(stored.contentType());
        coverImage.setSizeBytes(stored.sizeBytes());
        coverImage.setSortOrder(0);
        coverImage.setCover(true);
        coverImage.setCreatedAt(OffsetDateTime.now());
        productImageRepository.save(coverImage);

        replaceLegacyCoverReference(product, mediaStorageService.publicUrl(stored.storedFilename()));
        productRepository.save(product);

        return toImagesResponse(product);
    }

    @Transactional
    public ProductImagesResponse uploadGallery(Long productId, List<MultipartFile> files) {
        Product product = requireProduct(productId);
        int nextSortOrder = productImageRepository.findByProductIdAndCoverFalseOrderBySortOrderAscIdAsc(productId).stream()
            .map(ProductImage::getSortOrder)
            .max(Integer::compareTo)
            .orElse(0);

        if (files != null) {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue;
                }
                ProductMediaStorageService.StoredFile stored = mediaStorageService.store(file);
                ProductImage galleryImage = new ProductImage();
                galleryImage.setProduct(product);
                galleryImage.setStoredFilename(stored.storedFilename());
                galleryImage.setOriginalFilename(stored.originalFilename());
                galleryImage.setContentType(stored.contentType());
                galleryImage.setSizeBytes(stored.sizeBytes());
                galleryImage.setSortOrder(++nextSortOrder);
                galleryImage.setCover(false);
                galleryImage.setCreatedAt(OffsetDateTime.now());
                productImageRepository.save(galleryImage);
            }
        }

        return toImagesResponse(product);
    }

    @Transactional
    public ProductImagesResponse deleteImage(Long productId, Long imageId) {
        Product product = requireProduct(productId);
        ProductImage image = productImageRepository.findById(imageId)
            .filter(candidate -> candidate.getProduct().getId().equals(productId))
            .orElseThrow(() -> new NotFoundException("Product image not found"));

        boolean wasCover = Boolean.TRUE.equals(image.getCover());
        String storedFilename = image.getStoredFilename();

        productImageRepository.delete(image);
        mediaStorageService.deleteQuietly(storedFilename);

        if (wasCover) {
            product.setImage(null);
            productRepository.save(product);
        }

        return toImagesResponse(product);
    }

    @Transactional
    public ProductImagesResponse setCover(Long productId, Long imageId) {
        Product product = requireProduct(productId);

        ProductImage target = productImageRepository.findById(imageId)
            .filter(candidate -> candidate.getProduct().getId().equals(productId))
            .orElseThrow(() -> new NotFoundException("Product image not found"));

        productImageRepository.findFirstByProductIdAndCoverTrue(productId).ifPresent(current -> {
            if (!current.getId().equals(target.getId())) {
                current.setCover(false);
                productImageRepository.save(current);
            }
        });

        target.setCover(true);
        target.setSortOrder(0);
        productImageRepository.save(target);

        replaceLegacyCoverReference(product, mediaStorageService.publicUrl(target.getStoredFilename()));
        productRepository.save(product);

        return toImagesResponse(product);
    }

    @Transactional
    public void cleanupAllProductMedia(Product product) {
        if (product == null || product.getId() == null) {
            return;
        }

        List<ProductImage> images = productImageRepository.findByProductIdOrderBySortOrderAscIdAsc(product.getId());
        for (ProductImage image : images) {
            mediaStorageService.deleteQuietly(image.getStoredFilename());
        }

        String legacyImage = product.getImage();
        if (mediaStorageService.isManagedMediaUrl(legacyImage)) {
            String storedFilename = mediaStorageService.extractStoredFilename(legacyImage);
            mediaStorageService.deleteQuietly(storedFilename);
        }
    }

    @Transactional(readOnly = true)
    public ProductImageResponse resolveCoverImage(Long productId) {
        return productImageRepository.findFirstByProductIdAndCoverTrue(productId)
            .map(this::toImageResponse)
            .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ProductImageResponse> resolveGalleryImages(Long productId) {
        return productImageRepository.findByProductIdAndCoverFalseOrderBySortOrderAscIdAsc(productId).stream()
            .map(this::toImageResponse)
            .collect(Collectors.toList());
    }

    private Product requireProduct(Long productId) {
        return productRepository.findById(productId)
            .orElseThrow(() -> new NotFoundException("Product not found"));
    }

    private void replaceLegacyCoverReference(Product product, String newCoverUrl) {
        product.setImage(newCoverUrl);
    }

    private ProductImagesResponse toImagesResponse(Product product) {
        Long productId = product.getId();
        List<ProductImageResponse> all = productImageRepository.findByProductIdOrderBySortOrderAscIdAsc(productId).stream()
            .sorted(Comparator
                .comparing((ProductImage image) -> !Boolean.TRUE.equals(image.getCover()))
                .thenComparing(ProductImage::getSortOrder)
                .thenComparing(ProductImage::getId))
            .map(this::toImageResponse)
            .collect(Collectors.toList());

        ProductImageResponse cover = all.stream().filter(image -> Boolean.TRUE.equals(image.getCover())).findFirst().orElse(null);
        List<ProductImageResponse> gallery = all.stream().filter(image -> !Boolean.TRUE.equals(image.getCover())).collect(Collectors.toList());

        ProductImagesResponse response = new ProductImagesResponse();
        response.setCoverImage(cover);
        response.setGalleryImages(gallery);
        return response;
    }

    private ProductImageResponse toImageResponse(ProductImage image) {
        ProductImageResponse response = new ProductImageResponse();
        response.setId(image.getId());
        response.setUrl(mediaStorageService.publicUrl(image.getStoredFilename()));
        response.setOriginalFilename(image.getOriginalFilename());
        response.setContentType(image.getContentType());
        response.setSizeBytes(image.getSizeBytes());
        response.setSortOrder(image.getSortOrder());
        response.setCover(image.getCover());
        response.setCreatedAt(image.getCreatedAt());
        return response;
    }
}
