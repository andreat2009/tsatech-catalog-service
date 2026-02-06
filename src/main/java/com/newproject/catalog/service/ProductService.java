package com.newproject.catalog.service;

import com.newproject.catalog.domain.Category;
import com.newproject.catalog.domain.Manufacturer;
import com.newproject.catalog.domain.Product;
import com.newproject.catalog.dto.ProductRequest;
import com.newproject.catalog.dto.ProductResponse;
import com.newproject.catalog.events.CatalogEventPublisher;
import com.newproject.catalog.exception.BadRequestException;
import com.newproject.catalog.exception.NotFoundException;
import com.newproject.catalog.repository.CategoryRepository;
import com.newproject.catalog.repository.ManufacturerRepository;
import com.newproject.catalog.repository.ProductRepository;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
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

    public ProductService(
        ProductRepository productRepository,
        CategoryRepository categoryRepository,
        ManufacturerRepository manufacturerRepository,
        CatalogEventPublisher eventPublisher
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.manufacturerRepository = manufacturerRepository;
        this.eventPublisher = eventPublisher;
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
        eventPublisher.publish("PRODUCT_CREATED", "product", saved.getId().toString(), toResponse(saved));
        return toResponse(saved);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Product not found"));

        if (request.getSku() != null) {
            productRepository.findBySku(request.getSku())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> { throw new BadRequestException("SKU already exists"); });
        }

        applyRequest(product, request);
        product.setUpdatedAt(OffsetDateTime.now());

        Product saved = productRepository.save(product);
        eventPublisher.publish("PRODUCT_UPDATED", "product", saved.getId().toString(), toResponse(saved));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Product not found"));
        return toResponse(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> list() {
        return productRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Product not found"));
        productRepository.delete(product);
        eventPublisher.publish("PRODUCT_DELETED", "product", id.toString(), null);
    }

    private void applyRequest(Product product, ProductRequest request) {
        product.setSku(request.getSku());
        product.setModel(request.getModel());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setQuantity(request.getQuantity());
        product.setActive(request.getActive());
        product.setImage(request.getImage());

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
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setSku(product.getSku());
        response.setModel(product.getModel());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setQuantity(product.getQuantity());
        response.setActive(product.getActive());
        response.setImage(product.getImage());
        response.setManufacturerId(product.getManufacturer() != null ? product.getManufacturer().getId() : null);
        response.setCategoryIds(product.getCategories().stream().map(Category::getId).collect(Collectors.toSet()));
        response.setCreatedAt(product.getCreatedAt());
        response.setUpdatedAt(product.getUpdatedAt());
        return response;
    }
}
