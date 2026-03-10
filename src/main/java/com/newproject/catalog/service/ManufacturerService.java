package com.newproject.catalog.service;

import com.newproject.catalog.domain.Manufacturer;
import com.newproject.catalog.domain.ManufacturerTranslation;
import com.newproject.catalog.dto.LocalizedContent;
import com.newproject.catalog.dto.ManufacturerRequest;
import com.newproject.catalog.dto.ManufacturerResponse;
import com.newproject.catalog.events.CatalogEventPublisher;
import com.newproject.catalog.exception.BadRequestException;
import com.newproject.catalog.exception.NotFoundException;
import com.newproject.catalog.repository.ManufacturerRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
        String defaultName = resolveDefaultName(request, null);
        manufacturerRepository.findByName(defaultName)
            .ifPresent(existing -> {
                throw new BadRequestException("Manufacturer already exists");
            });

        Manufacturer manufacturer = new Manufacturer();
        applyRequest(manufacturer, request);
        Manufacturer saved = manufacturerRepository.save(manufacturer);
        ManufacturerResponse response = toResponse(saved, LanguageSupport.DEFAULT_LANGUAGE);
        eventPublisher.publish("MANUFACTURER_CREATED", "manufacturer", saved.getId().toString(), response);
        return response;
    }

    @Transactional
    public ManufacturerResponse update(Long id, ManufacturerRequest request) {
        Manufacturer manufacturer = manufacturerRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Manufacturer not found"));

        String defaultName = resolveDefaultName(request, manufacturer.getName());
        manufacturerRepository.findByName(defaultName)
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> {
                throw new BadRequestException("Manufacturer already exists");
            });

        applyRequest(manufacturer, request);
        Manufacturer saved = manufacturerRepository.save(manufacturer);
        ManufacturerResponse response = toResponse(saved, LanguageSupport.DEFAULT_LANGUAGE);
        eventPublisher.publish("MANUFACTURER_UPDATED", "manufacturer", saved.getId().toString(), response);
        return response;
    }

    @Transactional(readOnly = true)
    public ManufacturerResponse get(Long id, String language) {
        Manufacturer manufacturer = manufacturerRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Manufacturer not found"));
        return toResponse(manufacturer, language);
    }

    @Transactional(readOnly = true)
    public List<ManufacturerResponse> list(String language) {
        return manufacturerRepository.findAll().stream()
            .map(manufacturer -> toResponse(manufacturer, language))
            .sorted(Comparator.comparing(ManufacturerResponse::getName, String.CASE_INSENSITIVE_ORDER))
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
        Map<String, LocalizedContent> normalizedTranslations = normalizeTranslations(request.getTranslations(), request.getName(), manufacturer.getName());
        String defaultName = normalizedTranslations.get(LanguageSupport.DEFAULT_LANGUAGE).getName();

        manufacturer.setName(defaultName);
        manufacturer.setImage(request.getImage());
        manufacturer.setActive(request.getActive());

        syncTranslations(manufacturer, normalizedTranslations);
    }

    private void syncTranslations(Manufacturer manufacturer, Map<String, LocalizedContent> localizedContents) {
        Map<String, ManufacturerTranslation> existingByLanguage = manufacturer.getTranslations().stream()
            .collect(Collectors.toMap(
                translation -> translation.getLanguageCode().toLowerCase(Locale.ROOT),
                translation -> translation,
                (first, ignored) -> first
            ));

        for (String language : LanguageSupport.SUPPORTED_LANGUAGES) {
            LocalizedContent localizedContent = localizedContents.get(language);
            ManufacturerTranslation translation = existingByLanguage.get(language);
            if (translation == null) {
                translation = new ManufacturerTranslation();
                translation.setManufacturer(manufacturer);
                translation.setLanguageCode(language);
                manufacturer.getTranslations().add(translation);
                existingByLanguage.put(language, translation);
            }
            translation.setName(localizedContent.getName());
        }

        manufacturer.getTranslations().removeIf(translation ->
            !LanguageSupport.SUPPORTED_LANGUAGES.contains(translation.getLanguageCode().toLowerCase(Locale.ROOT)));
    }

    private ManufacturerResponse toResponse(Manufacturer manufacturer, String language) {
        String resolvedLanguage = LanguageSupport.normalizeLanguage(language);
        if (resolvedLanguage == null) {
            resolvedLanguage = LanguageSupport.DEFAULT_LANGUAGE;
        }

        Map<String, LocalizedContent> translations = toTranslationMap(manufacturer.getTranslations(), manufacturer.getName());
        LocalizedContent localized = translations.getOrDefault(resolvedLanguage, translations.get(LanguageSupport.DEFAULT_LANGUAGE));

        ManufacturerResponse response = new ManufacturerResponse();
        response.setId(manufacturer.getId());
        response.setName(localized != null ? localized.getName() : manufacturer.getName());
        response.setImage(manufacturer.getImage());
        response.setActive(manufacturer.getActive());
        response.setTranslations(translations);
        return response;
    }

    private Map<String, LocalizedContent> normalizeTranslations(Map<String, LocalizedContent> requested, String fallbackName, String existingName) {
        Map<String, LocalizedContent> normalized = new LinkedHashMap<>();

        String defaultName = firstNonBlank(
            extractName(requested, LanguageSupport.DEFAULT_LANGUAGE),
            fallbackName,
            existingName
        );

        if (defaultName == null || defaultName.isBlank()) {
            throw new BadRequestException("Manufacturer name is required");
        }

        for (String language : LanguageSupport.SUPPORTED_LANGUAGES) {
            LocalizedContent content = new LocalizedContent();
            String name = firstNonBlank(
                extractName(requested, language),
                language.equals(LanguageSupport.DEFAULT_LANGUAGE) ? fallbackName : null,
                defaultName
            );
            content.setName(name != null ? name : defaultName);
            normalized.put(language, content);
        }

        return normalized;
    }

    private Map<String, LocalizedContent> toTranslationMap(List<ManufacturerTranslation> translations, String fallbackName) {
        Map<String, LocalizedContent> map = new LinkedHashMap<>();
        Map<String, ManufacturerTranslation> byLanguage = translations.stream()
            .collect(Collectors.toMap(
                translation -> translation.getLanguageCode().toLowerCase(Locale.ROOT),
                translation -> translation,
                (first, ignored) -> first
            ));

        for (String language : LanguageSupport.SUPPORTED_LANGUAGES) {
            ManufacturerTranslation translation = byLanguage.get(language);
            LocalizedContent content = new LocalizedContent();
            content.setName(firstNonBlank(
                translation != null ? translation.getName() : null,
                language.equals(LanguageSupport.DEFAULT_LANGUAGE) ? fallbackName : null,
                fallbackName
            ));
            map.put(language, content);
        }

        return map;
    }

    private String resolveDefaultName(ManufacturerRequest request, String existingName) {
        return firstNonBlank(extractName(request.getTranslations(), LanguageSupport.DEFAULT_LANGUAGE), request.getName(), existingName);
    }

    private String extractName(Map<String, LocalizedContent> requested, String language) {
        if (requested == null) {
            return null;
        }
        LocalizedContent content = requested.get(language);
        if (content == null) {
            return null;
        }
        return trimToNull(content.getName());
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
