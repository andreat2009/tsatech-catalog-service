package com.newproject.catalog.service;

import com.newproject.catalog.domain.Category;
import com.newproject.catalog.domain.Manufacturer;
import com.newproject.catalog.domain.Product;
import com.newproject.catalog.dto.ProductImageResponse;
import com.newproject.catalog.dto.ProductRequest;
import com.newproject.catalog.dto.ProductResponse;
import com.newproject.catalog.events.CatalogEventPublisher;
import com.newproject.catalog.exception.BadRequestException;
import com.newproject.catalog.exception.NotFoundException;
import com.newproject.catalog.repository.CategoryRepository;
import com.newproject.catalog.repository.ManufacturerRepository;
import com.newproject.catalog.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final CatalogEventPublisher eventPublisher;
    private final ProductImageService productImageService;

    public ProductService(
        ProductRepository productRepository,
        CategoryRepository categoryRepository,
        ManufacturerRepository manufacturerRepository,
        CatalogEventPublisher eventPublisher,
        ProductImageService productImageService
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.manufacturerRepository = manufacturerRepository;
        this.eventPublisher = eventPublisher;
        this.productImageService = productImageService;
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (request.getSku() != null && productRepository.findBySku(request.getSku()).isPresent()) {
            throw new BadRequestException("SKU already exists");
        }

        Product product = new Product();
        applyRequest(product, request);
        OffsetDateTime now = OffsetDateTime.now();
        product.setCreatedAt(now);
        product.setUpdatedAt(now);

        Product saved = productRepository.save(product);
        ProductResponse response = toResponse(saved);
        eventPublisher.publish("PRODUCT_CREATED", "product", saved.getId().toString(), response);
        return response;
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Product not found"));

        if (request.getSku() != null) {
            productRepository.findBySku(request.getSku())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BadRequestException("SKU already exists");
                });
        }

        applyRequest(product, request);
        product.setUpdatedAt(OffsetDateTime.now());

        Product saved = productRepository.save(product);
        ProductResponse response = toResponse(saved);
        eventPublisher.publish("PRODUCT_UPDATED", "product", saved.getId().toString(), response);
        return response;
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Product not found"));
        return toResponse(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> list() {
        return list(null, null, null, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> list(
        String query,
        Long categoryId,
        Boolean active,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String sort
    ) {
        List<Product> products = active != null
            ? productRepository.findByActive(active)
            : productRepository.findAll();

        String normalizedQuery = query != null ? query.trim().toLowerCase(Locale.ROOT) : null;
        if (normalizedQuery != null && !normalizedQuery.isEmpty()) {
            products = products.stream()
                .filter(product -> contains(product.getSku(), normalizedQuery)
                    || contains(product.getModel(), normalizedQuery)
                    || contains(product.getName(), normalizedQuery)
                    || contains(product.getDescription(), normalizedQuery))
                .collect(Collectors.toList());
        }

        if (categoryId != null) {
            products = products.stream()
                .filter(product -> product.getCategories().stream().anyMatch(category -> categoryId.equals(category.getId())))
                .collect(Collectors.toList());
        }

        if (minPrice != null) {
            products = products.stream()
                .filter(product -> product.getPrice() != null && product.getPrice().compareTo(minPrice) >= 0)
                .collect(Collectors.toList());
        }

        if (maxPrice != null) {
            products = products.stream()
                .filter(product -> product.getPrice() != null && product.getPrice().compareTo(maxPrice) <= 0)
                .collect(Collectors.toList());
        }

        products.sort(resolveComparator(sort));

        return products.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Product not found"));

        productImageService.cleanupAllProductMedia(product);
        productRepository.delete(product);
        eventPublisher.publish("PRODUCT_DELETED", "product", id.toString(), null);
    }

    private Comparator<Product> resolveComparator(String sort) {
        String normalizedSort = sort != null ? sort.toLowerCase(Locale.ROOT) : "";

        return switch (normalizedSort) {
            case "name_desc" -> Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER)
                .reversed()
                .thenComparing(Product::getId);
            case "price_asc" -> Comparator.comparing(Product::getPrice, Comparator.nullsLast(BigDecimal::compareTo))
                .thenComparing(Product::getId);
            case "price_desc" -> Comparator.comparing(Product::getPrice, Comparator.nullsLast(BigDecimal::compareTo))
                .reversed()
                .thenComparing(Product::getId);
            case "newest" -> Comparator.comparing(Product::getId, Comparator.nullsLast(Long::compareTo)).reversed();
            case "name_asc" -> Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Product::getId);
            default -> Comparator.comparing(Product::getId, Comparator.nullsLast(Long::compareTo));
        };
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private void applyRequest(Product product, ProductRequest request) {
        product.setSku(request.getSku());
        product.setModel(request.getModel());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setQuantity(request.getQuantity());
        product.setActive(request.getActive());

        if (request.getImage() != null) {
            product.setImage(request.getImage());
        }

        if (request.getManufacturerId() != null) {
            Manufacturer manufacturer = manufacturerRepository.findById(request.getManufacturerId())
                .orElseThrow(() -> new BadRequestException("Manufacturer not found"));
            product.setManufacturer(manufacturer);
        } else {
            product.setManufacturer(null);
        }

        if (request.getCategoryIds() != null) {
            Set<Category> categories = new HashSet<>(categoryRepository.findAllById(request.getCategoryIds()));
            if (categories.size() != request.getCategoryIds().size()) {
                throw new BadRequestException("One or more categories not found");
            }
            product.setCategories(categories);
        }
    }

    private ProductResponse toResponse(Product product) {
        ProductImageResponse coverImage = productImageService.resolveCoverImage(product.getId());

        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setSku(product.getSku());
        response.setModel(product.getModel());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setQuantity(product.getQuantity());
        response.setActive(product.getActive());

        String coverUrl = resolveDisplayImageUrl(coverImage, product.getImage());
        response.setImage(coverUrl);
        response.setCoverImageUrl(coverUrl);
        response.setGalleryImages(productImageService.resolveGalleryImages(product.getId()));

        response.setManufacturerId(product.getManufacturer() != null ? product.getManufacturer().getId() : null);
        response.setCategoryIds(product.getCategories().stream().map(Category::getId).collect(Collectors.toSet()));
        response.setCreatedAt(product.getCreatedAt());
        response.setUpdatedAt(product.getUpdatedAt());
        return response;
    }

    private String resolveDisplayImageUrl(ProductImageResponse coverImage, String legacyImageUrl) {
        if (coverImage != null && coverImage.getUrl() != null && !coverImage.getUrl().isBlank()) {
            return coverImage.getUrl();
        }
        if (legacyImageUrl == null || legacyImageUrl.isBlank()) {
            return null;
        }
        if (legacyImageUrl.startsWith("http://") || legacyImageUrl.startsWith("https://")) {
            return legacyImageUrl;
        }
        if (legacyImageUrl.startsWith("/api/catalog/media/")) {
            return legacyImageUrl;
        }
        return null;
    }
}
