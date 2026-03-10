package com.newproject.catalog.controller;

import com.newproject.catalog.dto.CategoryRequest;
import com.newproject.catalog.dto.CategoryResponse;
import com.newproject.catalog.dto.CategoryTreeResponse;
import com.newproject.catalog.service.CategoryService;
import com.newproject.catalog.service.LanguageSupport;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalog/categories")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<CategoryResponse> list(
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false) String lang,
        @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage
    ) {
        String resolvedLanguage = LanguageSupport.resolveLanguage(lang, acceptLanguage);
        return categoryService.list(active, resolvedLanguage);
    }

    @GetMapping("/tree")
    public List<CategoryTreeResponse> tree(
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false) String lang,
        @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage
    ) {
        String resolvedLanguage = LanguageSupport.resolveLanguage(lang, acceptLanguage);
        return categoryService.tree(active, resolvedLanguage);
    }

    @GetMapping("/{id}")
    public CategoryResponse get(
        @PathVariable Long id,
        @RequestParam(required = false) String lang,
        @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage
    ) {
        String resolvedLanguage = LanguageSupport.resolveLanguage(lang, acceptLanguage);
        return categoryService.get(id, resolvedLanguage);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CategoryRequest request) {
        return categoryService.create(request);
    }

    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return categoryService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        categoryService.delete(id);
    }
}
