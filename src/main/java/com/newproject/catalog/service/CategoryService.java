package com.newproject.catalog.service;

import com.newproject.catalog.domain.Category;
import com.newproject.catalog.domain.CategoryTranslation;
import com.newproject.catalog.dto.CategoryRequest;
import com.newproject.catalog.dto.CategoryResponse;
import com.newproject.catalog.dto.CategoryTreeResponse;
import com.newproject.catalog.dto.LocalizedContent;
import com.newproject.catalog.events.CatalogEventPublisher;
import com.newproject.catalog.exception.BadRequestException;
import com.newproject.catalog.exception.NotFoundException;
import com.newproject.catalog.repository.CategoryRepository;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
        CategoryResponse response = toResponse(saved, LanguageSupport.DEFAULT_LANGUAGE);
        eventPublisher.publish("CATEGORY_CREATED", "category", saved.getId().toString(), response);
        return response;
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Category not found"));
        applyRequest(category, request);
        Category saved = categoryRepository.save(category);
        CategoryResponse response = toResponse(saved, LanguageSupport.DEFAULT_LANGUAGE);
        eventPublisher.publish("CATEGORY_UPDATED", "category", saved.getId().toString(), response);
        return response;
    }

    @Transactional(readOnly = true)
    public CategoryResponse get(Long id, String language) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Category not found"));
        return toResponse(category, language);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list(Boolean active, String language) {
        List<Category> categories = active != null
            ? categoryRepository.findByActiveOrderBySortOrderAscNameAsc(active)
            : categoryRepository.findAll();

        String resolvedLanguage = LanguageSupport.normalizeLanguage(language);
        if (resolvedLanguage == null) {
            resolvedLanguage = LanguageSupport.DEFAULT_LANGUAGE;
        }

        String finalResolvedLanguage = resolvedLanguage;
        categories.sort(Comparator
            .comparing(Category::getSortOrder)
            .thenComparing(category -> resolveLocalizedContent(category.getTranslations(), finalResolvedLanguage, category.getName(), category.getDescription()).getName(), String.CASE_INSENSITIVE_ORDER)
        );

        return categories.stream()
            .map(category -> toResponse(category, finalResolvedLanguage))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryTreeResponse> tree(Boolean active, String language) {
        List<Category> categories = active != null
            ? categoryRepository.findByActiveOrderBySortOrderAscNameAsc(active)
            : categoryRepository.findAll();

        String resolvedLanguage = LanguageSupport.normalizeLanguage(language);
        if (resolvedLanguage == null) {
            resolvedLanguage = LanguageSupport.DEFAULT_LANGUAGE;
        }

        String finalResolvedLanguage = resolvedLanguage;
        categories.sort(Comparator
            .comparing(Category::getSortOrder)
            .thenComparing(category -> resolveLocalizedContent(category.getTranslations(), finalResolvedLanguage, category.getName(), category.getDescription()).getName(), String.CASE_INSENSITIVE_ORDER)
        );

        Map<Long, CategoryTreeResponse> nodes = new HashMap<>();
        for (Category category : categories) {
            LocalizedContent localized = resolveLocalizedContent(category.getTranslations(), finalResolvedLanguage, category.getName(), category.getDescription());
            CategoryTreeResponse node = new CategoryTreeResponse();
            node.setId(category.getId());
            node.setName(localized.getName());
            node.setDescription(localized.getDescription());
            node.setSortOrder(category.getSortOrder());
            node.setTranslations(toLocalizedContentMap(category.getTranslations(), category.getName(), category.getDescription()));
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
        Map<String, LocalizedContent> normalizedTranslations = normalizeLocalizedContent(
            request.getTranslations(),
            request.getName(),
            request.getDescription(),
            category.getName(),
            category.getDescription(),
            "Category name is required"
        );

        LocalizedContent defaultContent = normalizedTranslations.get(LanguageSupport.DEFAULT_LANGUAGE);
        category.setName(defaultContent.getName());
        category.setDescription(defaultContent.getDescription());
        syncTranslations(category, normalizedTranslations);

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

    private void syncTranslations(Category category, Map<String, LocalizedContent> localizedContents) {
        Map<String, CategoryTranslation> existingByLanguage = category.getTranslations().stream()
            .collect(Collectors.toMap(
                translation -> translation.getLanguageCode().toLowerCase(Locale.ROOT),
                translation -> translation,
                (first, ignored) -> first
            ));

        for (String language : LanguageSupport.SUPPORTED_LANGUAGES) {
            LocalizedContent localizedContent = localizedContents.get(language);
            CategoryTranslation translation = existingByLanguage.get(language);
            if (translation == null) {
                translation = new CategoryTranslation();
                translation.setCategory(category);
                translation.setLanguageCode(language);
                category.getTranslations().add(translation);
                existingByLanguage.put(language, translation);
            }
            translation.setName(localizedContent.getName());
            translation.setDescription(localizedContent.getDescription());
        }

        category.getTranslations().removeIf(translation ->
            !LanguageSupport.SUPPORTED_LANGUAGES.contains(translation.getLanguageCode().toLowerCase(Locale.ROOT)));
    }

    private void sortTree(List<CategoryTreeResponse> nodes) {
        nodes.sort(Comparator.comparing(CategoryTreeResponse::getSortOrder).thenComparing(CategoryTreeResponse::getName));
        for (CategoryTreeResponse node : nodes) {
            sortTree(node.getChildren());
        }
    }

    private CategoryResponse toResponse(Category category, String language) {
        LocalizedContent localized = resolveLocalizedContent(category.getTranslations(), language, category.getName(), category.getDescription());
        CategoryResponse response = new CategoryResponse();
        response.setId(category.getId());
        response.setParentId(category.getParent() != null ? category.getParent().getId() : null);
        response.setName(localized.getName());
        response.setDescription(localized.getDescription());
        response.setActive(category.getActive());
        response.setSortOrder(category.getSortOrder());
        response.setTranslations(toLocalizedContentMap(category.getTranslations(), category.getName(), category.getDescription()));
        return response;
    }

    private Map<String, LocalizedContent> toLocalizedContentMap(List<CategoryTranslation> translations, String fallbackName, String fallbackDescription) {
        Map<String, LocalizedContent> map = new LinkedHashMap<>();
        Map<String, CategoryTranslation> byLanguage = translations.stream()
            .collect(Collectors.toMap(
                translation -> translation.getLanguageCode().toLowerCase(Locale.ROOT),
                translation -> translation,
                (first, ignored) -> first
            ));

        for (String language : LanguageSupport.SUPPORTED_LANGUAGES) {
            CategoryTranslation translation = byLanguage.get(language);
            LocalizedContent content = new LocalizedContent();
            content.setName(firstNonBlank(
                translation != null ? translation.getName() : null,
                language.equals(LanguageSupport.DEFAULT_LANGUAGE) ? fallbackName : null,
                fallbackName
            ));
            content.setDescription(firstNonBlank(
                translation != null ? translation.getDescription() : null,
                language.equals(LanguageSupport.DEFAULT_LANGUAGE) ? fallbackDescription : null,
                fallbackDescription
            ));
            map.put(language, content);
        }

        return map;
    }

    private LocalizedContent resolveLocalizedContent(
        List<CategoryTranslation> translations,
        String language,
        String fallbackName,
        String fallbackDescription
    ) {
        String resolvedLanguage = LanguageSupport.normalizeLanguage(language);
        if (resolvedLanguage == null) {
            resolvedLanguage = LanguageSupport.DEFAULT_LANGUAGE;
        }

        Map<String, LocalizedContent> map = toLocalizedContentMap(translations, fallbackName, fallbackDescription);
        LocalizedContent localized = map.get(resolvedLanguage);
        if (localized == null) {
            localized = map.get(LanguageSupport.DEFAULT_LANGUAGE);
        }
        if (localized == null) {
            localized = new LocalizedContent();
            localized.setName(fallbackName);
            localized.setDescription(fallbackDescription);
        }
        return localized;
    }

    private Map<String, LocalizedContent> normalizeLocalizedContent(
        Map<String, LocalizedContent> requested,
        String fallbackName,
        String fallbackDescription,
        String existingName,
        String existingDescription,
        String requiredNameMessage
    ) {
        Map<String, LocalizedContent> normalized = new LinkedHashMap<>();

        String defaultName = firstNonBlank(
            extractValue(requested, LanguageSupport.DEFAULT_LANGUAGE, true),
            fallbackName,
            existingName
        );
        String defaultDescription = firstNonBlank(
            extractValue(requested, LanguageSupport.DEFAULT_LANGUAGE, false),
            fallbackDescription,
            existingDescription
        );

        if (defaultName == null || defaultName.isBlank()) {
            throw new BadRequestException(requiredNameMessage);
        }

        for (String language : LanguageSupport.SUPPORTED_LANGUAGES) {
            LocalizedContent content = new LocalizedContent();
            String name = firstNonBlank(
                extractValue(requested, language, true),
                language.equals(LanguageSupport.DEFAULT_LANGUAGE) ? fallbackName : null,
                defaultName
            );
            String description = firstNonBlank(
                extractValue(requested, language, false),
                language.equals(LanguageSupport.DEFAULT_LANGUAGE) ? fallbackDescription : null,
                defaultDescription
            );
            content.setName(name != null ? name : defaultName);
            content.setDescription(description);
            normalized.put(language, content);
        }

        return normalized;
    }

    private String extractValue(Map<String, LocalizedContent> requested, String language, boolean nameField) {
        if (requested == null) {
            return null;
        }
        LocalizedContent content = requested.get(language);
        if (content == null) {
            return null;
        }
        return trimToNull(nameField ? content.getName() : content.getDescription());
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String trimmed = trimToNull(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
