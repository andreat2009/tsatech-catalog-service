package com.newproject.catalog.controller;

import com.newproject.catalog.dto.ManufacturerRequest;
import com.newproject.catalog.dto.ManufacturerResponse;
import com.newproject.catalog.service.ManufacturerService;
import jakarta.validation.Valid;
import java.util.List;
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
    public List<ManufacturerResponse> list() {
        return manufacturerService.list();
    }

    @GetMapping("/{id}")
    public ManufacturerResponse get(@PathVariable Long id) {
        return manufacturerService.get(id);
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
