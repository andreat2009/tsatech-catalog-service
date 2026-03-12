package com.newproject.catalog.controller;

import com.newproject.catalog.dto.ProductAutoTranslateRequest;
import com.newproject.catalog.dto.ProductAutoTranslateResponse;
import com.newproject.catalog.dto.ProductRequest;
import com.newproject.catalog.dto.ProductResponse;
import com.newproject.catalog.service.LanguageSupport;
import com.newproject.catalog.service.ProductService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalog/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductResponse> list(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(required = false) String sort,
        @RequestParam(required = false) String lang,
        @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage
    ) {
        String resolvedLanguage = LanguageSupport.resolveLanguage(lang, acceptLanguage);
        return productService.list(q, categoryId, active, minPrice, maxPrice, sort, resolvedLanguage);
    }

    @GetMapping("/{id}")
    public ProductResponse get(
        @PathVariable Long id,
        @RequestParam(required = false) String lang,
        @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage
    ) {
        String resolvedLanguage = LanguageSupport.resolveLanguage(lang, acceptLanguage);
        return productService.get(id, resolvedLanguage);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return productService.create(request);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }


    @PostMapping("/translate")
    public ProductAutoTranslateResponse autoTranslate(@RequestBody ProductAutoTranslateRequest request) {
        return productService.autoTranslate(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        productService.delete(id);
    }
}
