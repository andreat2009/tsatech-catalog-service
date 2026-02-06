package com.newproject.catalog.service;

import com.newproject.catalog.domain.Category;
import com.newproject.catalog.dto.CategoryRequest;
import com.newproject.catalog.dto.CategoryResponse;
import com.newproject.catalog.events.CatalogEventPublisher;
import com.newproject.catalog.exception.BadRequestException;
import com.newproject.catalog.exception.NotFoundException;
import com.newproject.catalog.repository.CategoryRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final CatalogEventPublisher eventPublisher;

    public CategoryService(CategoryRepository categoryRepository, CatalogEventPublisher eventPublisher) {
        this.categoryRepository = categoryRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        Category category = new Category();
        applyRequest(category, request);
        Category saved = categoryRepository.save(category);
        eventPublisher.publish("CATEGORY_CREATED", "category", saved.getId().toString(), toResponse(saved));
        return toResponse(saved);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Category not found"));
        applyRequest(category, request);
        Category saved = categoryRepository.save(category);
        eventPublisher.publish("CATEGORY_UPDATED", "category", saved.getId().toString(), toResponse(saved));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CategoryResponse get(Long id) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Category not found"));
        return toResponse(category);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        return categoryRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Category not found"));
        categoryRepository.delete(category);
        eventPublisher.publish("CATEGORY_DELETED", "category", id.toString(), null);
    }

    private void applyRequest(Category category, CategoryRequest request) {
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setActive(request.getActive());
        category.setSortOrder(request.getSortOrder());

        if (request.getParentId() != null) {
            if (request.getParentId().equals(category.getId())) {
                throw new BadRequestException("Category cannot be parent of itself");
            }
            Category parent = categoryRepository.findById(request.getParentId())
                .orElseThrow(() -> new BadRequestException("Parent category not found"));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }
    }

    private CategoryResponse toResponse(Category category) {
        CategoryResponse response = new CategoryResponse();
        response.setId(category.getId());
        response.setParentId(category.getParent() != null ? category.getParent().getId() : null);
        response.setName(category.getName());
        response.setDescription(category.getDescription());
        response.setActive(category.getActive());
        response.setSortOrder(category.getSortOrder());
        return response;
    }
}
