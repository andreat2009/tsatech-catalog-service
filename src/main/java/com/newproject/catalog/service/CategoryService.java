package com.newproject.catalog.service;

import com.newproject.catalog.domain.Category;
import com.newproject.catalog.dto.CategoryRequest;
import com.newproject.catalog.dto.CategoryResponse;
import com.newproject.catalog.dto.CategoryTreeResponse;
import com.newproject.catalog.events.CatalogEventPublisher;
import com.newproject.catalog.exception.BadRequestException;
import com.newproject.catalog.exception.NotFoundException;
import com.newproject.catalog.repository.CategoryRepository;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public List<CategoryResponse> list(Boolean active) {
        List<Category> categories = active != null
            ? categoryRepository.findByActiveOrderBySortOrderAscNameAsc(active)
            : categoryRepository.findAll();

        if (active == null) {
            categories.sort(Comparator.comparing(Category::getSortOrder).thenComparing(Category::getName));
        }

        return categories.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryTreeResponse> tree(Boolean active) {
        List<Category> categories = active != null
            ? categoryRepository.findByActiveOrderBySortOrderAscNameAsc(active)
            : categoryRepository.findAll();

        categories.sort(Comparator.comparing(Category::getSortOrder).thenComparing(Category::getName));

        Map<Long, CategoryTreeResponse> nodes = new HashMap<>();
        for (Category category : categories) {
            CategoryTreeResponse node = new CategoryTreeResponse();
            node.setId(category.getId());
            node.setName(category.getName());
            node.setDescription(category.getDescription());
            node.setSortOrder(category.getSortOrder());
            nodes.put(category.getId(), node);
        }

        List<CategoryTreeResponse> roots = categories.stream()
            .filter(category -> category.getParent() == null || !nodes.containsKey(category.getParent().getId()))
            .map(category -> nodes.get(category.getId()))
            .collect(Collectors.toList());

        for (Category category : categories) {
            if (category.getParent() == null) {
                continue;
            }
            CategoryTreeResponse parent = nodes.get(category.getParent().getId());
            CategoryTreeResponse child = nodes.get(category.getId());
            if (parent != null && child != null) {
                parent.getChildren().add(child);
            }
        }

        sortTree(roots);
        return roots;
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

    private void sortTree(List<CategoryTreeResponse> nodes) {
        nodes.sort(Comparator.comparing(CategoryTreeResponse::getSortOrder).thenComparing(CategoryTreeResponse::getName));
        for (CategoryTreeResponse node : nodes) {
            sortTree(node.getChildren());
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
