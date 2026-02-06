package com.newproject.catalog.service;

import com.newproject.catalog.domain.Manufacturer;
import com.newproject.catalog.dto.ManufacturerRequest;
import com.newproject.catalog.dto.ManufacturerResponse;
import com.newproject.catalog.events.CatalogEventPublisher;
import com.newproject.catalog.exception.BadRequestException;
import com.newproject.catalog.exception.NotFoundException;
import com.newproject.catalog.repository.ManufacturerRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManufacturerService {
    private final ManufacturerRepository manufacturerRepository;
    private final CatalogEventPublisher eventPublisher;

    public ManufacturerService(ManufacturerRepository manufacturerRepository, CatalogEventPublisher eventPublisher) {
        this.manufacturerRepository = manufacturerRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ManufacturerResponse create(ManufacturerRequest request) {
        manufacturerRepository.findByName(request.getName())
            .ifPresent(existing -> { throw new BadRequestException("Manufacturer already exists"); });

        Manufacturer manufacturer = new Manufacturer();
        applyRequest(manufacturer, request);
        Manufacturer saved = manufacturerRepository.save(manufacturer);
        eventPublisher.publish("MANUFACTURER_CREATED", "manufacturer", saved.getId().toString(), toResponse(saved));
        return toResponse(saved);
    }

    @Transactional
    public ManufacturerResponse update(Long id, ManufacturerRequest request) {
        Manufacturer manufacturer = manufacturerRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Manufacturer not found"));

        manufacturerRepository.findByName(request.getName())
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> { throw new BadRequestException("Manufacturer already exists"); });

        applyRequest(manufacturer, request);
        Manufacturer saved = manufacturerRepository.save(manufacturer);
        eventPublisher.publish("MANUFACTURER_UPDATED", "manufacturer", saved.getId().toString(), toResponse(saved));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ManufacturerResponse get(Long id) {
        Manufacturer manufacturer = manufacturerRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Manufacturer not found"));
        return toResponse(manufacturer);
    }

    @Transactional(readOnly = true)
    public List<ManufacturerResponse> list() {
        return manufacturerRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long id) {
        Manufacturer manufacturer = manufacturerRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Manufacturer not found"));
        manufacturerRepository.delete(manufacturer);
        eventPublisher.publish("MANUFACTURER_DELETED", "manufacturer", id.toString(), null);
    }

    private void applyRequest(Manufacturer manufacturer, ManufacturerRequest request) {
        manufacturer.setName(request.getName());
        manufacturer.setImage(request.getImage());
        manufacturer.setActive(request.getActive());
    }

    private ManufacturerResponse toResponse(Manufacturer manufacturer) {
        ManufacturerResponse response = new ManufacturerResponse();
        response.setId(manufacturer.getId());
        response.setName(manufacturer.getName());
        response.setImage(manufacturer.getImage());
        response.setActive(manufacturer.getActive());
        return response;
    }
}
