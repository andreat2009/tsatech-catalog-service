package com.newproject.catalog.controller;

import com.newproject.catalog.dto.ManufacturerRequest;
import com.newproject.catalog.dto.ManufacturerResponse;
import com.newproject.catalog.service.LanguageSupport;
import com.newproject.catalog.service.ManufacturerService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalog/manufacturers")
public class ManufacturerController {
    private final ManufacturerService manufacturerService;

    public ManufacturerController(ManufacturerService manufacturerService) {
        this.manufacturerService = manufacturerService;
    }

    @GetMapping
    public List<ManufacturerResponse> list(
        @RequestParam(required = false) String lang,
        @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage
    ) {
        String resolvedLanguage = LanguageSupport.resolveLanguage(lang, acceptLanguage);
        return manufacturerService.list(resolvedLanguage);
    }

    @GetMapping("/{id}")
    public ManufacturerResponse get(
        @PathVariable Long id,
        @RequestParam(required = false) String lang,
        @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage
    ) {
        String resolvedLanguage = LanguageSupport.resolveLanguage(lang, acceptLanguage);
        return manufacturerService.get(id, resolvedLanguage);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ManufacturerResponse create(@Valid @RequestBody ManufacturerRequest request) {
        return manufacturerService.create(request);
    }

    @PutMapping("/{id}")
    public ManufacturerResponse update(@PathVariable Long id, @Valid @RequestBody ManufacturerRequest request) {
        return manufacturerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        manufacturerService.delete(id);
    }
}
