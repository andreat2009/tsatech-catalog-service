package com.newproject.catalog.service;

import com.newproject.catalog.domain.Category;
import com.newproject.catalog.domain.Manufacturer;
import com.newproject.catalog.domain.Product;
import com.newproject.catalog.domain.ProductTranslation;
import com.newproject.catalog.domain.ProductVariant;
import com.newproject.catalog.dto.LocalizedContent;
import com.newproject.catalog.dto.ProductAutoTranslateRequest;
import com.newproject.catalog.dto.ProductAutoTranslateResponse;
import com.newproject.catalog.dto.ProductImageResponse;
import com.newproject.catalog.dto.ProductRequest;
import com.newproject.catalog.dto.ProductResponse;
import com.newproject.catalog.dto.ProductVariantRequest;
import com.newproject.catalog.dto.ProductVariantResponse;
import com.newproject.catalog.events.CatalogEventPublisher;
import com.newproject.catalog.exception.BadRequestException;
import com.newproject.catalog.exception.NotFoundException;
import com.newproject.catalog.repository.CategoryRepository;
import com.newproject.catalog.repository.ManufacturerRepository;
import com.newproject.catalog.repository.ProductRepository;
import com.newproject.catalog.service.translation.TranslationResult;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
    private static final Pattern VARIANT_KEY_PATTERN = Pattern.compile("^[A-Za-z0-9._-]+$");
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final CatalogEventPublisher eventPublisher;
    private final ProductImageService productImageService;
    private final CatalogTranslationService catalogTranslationService;

    public ProductService(
        ProductRepository productRepository,
        CategoryRepository categoryRepository,
        ManufacturerRepository manufacturerRepository,
        CatalogEventPublisher eventPublisher,
        ProductImageService productImageService,
        CatalogTranslationService catalogTranslationService
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.manufacturerRepository = manufacturerRepository;
        this.eventPublisher = eventPublisher;
        this.productImageService = productImageService;
        this.catalogTranslationService = catalogTranslationService;
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
        ProductResponse response = toResponse(saved, LanguageSupport.DEFAULT_LANGUAGE);
        eventPublisher.publish("PRODUCT_CREATED", "product", saved.getId().toString(), response);
        return response;
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Product not found"));

        if (request.getSku() != null) {
            productRepository.findBySku(request.getSku())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BadRequestException("SKU already exists");
                });
        }

        applyRequest(product, request);
        product.setUpdatedAt(OffsetDateTime.now());

        Product saved = productRepository.save(product);
        ProductResponse response = toResponse(saved, LanguageSupport.DEFAULT_LANGUAGE);
        eventPublisher.publish("PRODUCT_UPDATED", "product", saved.getId().toString(), response);
        return response;
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id, String language) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Product not found"));
        return toResponse(product, language);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> list() {
        return list(null, null, null, null, null, null, LanguageSupport.DEFAULT_LANGUAGE);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> list(
        String query,
        Long categoryId,
        Boolean active,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String sort,
        String language
    ) {
        String requestedLanguage = LanguageSupport.normalizeLanguage(language);
        final String resolvedLanguage = requestedLanguage != null ? requestedLanguage : LanguageSupport.DEFAULT_LANGUAGE;

        List<Product> products = active != null
            ? productRepository.findByActive(active)
            : productRepository.findAll();

        String normalizedQuery = query != null ? query.trim().toLowerCase(Locale.ROOT) : null;
        if (normalizedQuery != null && !normalizedQuery.isEmpty()) {
            String queryValue = normalizedQuery;
            products = products.stream()
                .filter(product -> {
                    LocalizedContent localized = resolveLocalizedContent(product.getTranslations(), resolvedLanguage, product.getName(), product.getDescription());
                    return contains(product.getSku(), queryValue)
                        || contains(product.getModel(), queryValue)
                        || contains(localized.getName(), queryValue)
                        || contains(localized.getDescription(), queryValue);
                })
                .collect(Collectors.toList());
        }

        if (categoryId != null) {
            products = products.stream()
                .filter(product -> product.getCategories().stream().anyMatch(category -> categoryId.equals(category.getId())))
                .collect(Collectors.toList());
        }

        if (minPrice != null) {
            products = products.stream()
                .filter(product -> product.getPrice() != null && product.getPrice().compareTo(minPrice) >= 0)
                .collect(Collectors.toList());
        }

        if (maxPrice != null) {
            products = products.stream()
                .filter(product -> product.getPrice() != null && product.getPrice().compareTo(maxPrice) <= 0)
                .collect(Collectors.toList());
        }

        products.sort(resolveComparator(sort, resolvedLanguage));

        String finalResolvedLanguage = resolvedLanguage;
        return products.stream()
            .map(product -> toResponse(product, finalResolvedLanguage))
            .collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Product not found"));

        productImageService.cleanupAllProductMedia(product);
        productRepository.delete(product);
        eventPublisher.publish("PRODUCT_DELETED", "product", id.toString(), null);
    }

    @Transactional(readOnly = true)
    public ProductAutoTranslateResponse autoTranslate(ProductAutoTranslateRequest request) {
        Map<String, LocalizedContent> normalized = normalizeTranslationPayload(
            request != null ? request.getTranslations() : null
        );

        String sourceLanguage = LanguageSupport.normalizeLanguage(request != null ? request.getSourceLanguage() : null);
        if (sourceLanguage == null) {
            sourceLanguage = LanguageSupport.DEFAULT_LANGUAGE;
        }

        LocalizedContent sourceContent = normalized.get(sourceLanguage);
        if (sourceContent == null) {
            sourceContent = new LocalizedContent();
            normalized.put(sourceLanguage, sourceContent);
        }

        if (trimToNull(sourceContent.getName()) == null) {
            throw new BadRequestException("Source language product name is required");
        }

        boolean overwrite = request != null && Boolean.TRUE.equals(request.getOverwriteExisting());
        Set<String> targets = new LinkedHashSet<>();
        for (String language : LanguageSupport.SUPPORTED_LANGUAGES) {
            if (language.equals(sourceLanguage)) {
                continue;
            }
            LocalizedContent current = normalized.get(language);
            if (overwrite || isBlank(current != null ? current.getName() : null) || isBlank(current != null ? current.getDescription() : null)) {
                targets.add(language);
            }
        }

        TranslationResult translationResult = catalogTranslationService.translateProductContent(sourceLanguage, sourceContent, targets);
        Set<String> translatedLanguages = new LinkedHashSet<>();

        if (translationResult.getTranslations() != null) {
            for (Map.Entry<String, LocalizedContent> entry : translationResult.getTranslations().entrySet()) {
                String language = LanguageSupport.normalizeLanguage(entry.getKey());
                if (language == null || language.equals(sourceLanguage)) {
                    continue;
                }

                LocalizedContent translated = entry.getValue();
                if (translated == null) {
                    continue;
                }

                LocalizedContent current = normalized.get(language);
                if (current == null) {
                    current = new LocalizedContent();
                    normalized.put(language, current);
                }

                boolean changed = false;
                String translatedName = trimToNull(translated.getName());
                String translatedDescription = trimToNull(translated.getDescription());

                if (translatedName != null && (overwrite || isBlank(current.getName()))) {
                    current.setName(translatedName);
                    changed = true;
                }
                if (translatedDescription != null && (overwrite || isBlank(current.getDescription()))) {
                    current.setDescription(translatedDescription);
                    changed = true;
                }

                if (changed) {
                    translatedLanguages.add(language);
                }
            }
        }

        ProductAutoTranslateResponse response = new ProductAutoTranslateResponse();
        response.setTranslations(normalized);
        response.setTranslatedLanguages(new ArrayList<>(translatedLanguages));
        response.setWarnings(translationResult.getWarnings() != null
            ? new ArrayList<>(translationResult.getWarnings())
            : new ArrayList<>());
        return response;
    }

    private Comparator<Product> resolveComparator(String sort, String language) {
        String normalizedSort = sort != null ? sort.toLowerCase(Locale.ROOT) : "";

        return switch (normalizedSort) {
            case "name_desc" -> Comparator.comparing((Product product) -> resolveLocalizedName(product, language), String.CASE_INSENSITIVE_ORDER)
                .reversed()
                .thenComparing(Product::getId);
            case "price_asc" -> Comparator.comparing(Product::getPrice, Comparator.nullsLast(BigDecimal::compareTo))
                .thenComparing(Product::getId);
            case "price_desc" -> Comparator.comparing(Product::getPrice, Comparator.nullsLast(BigDecimal::compareTo))
                .reversed()
                .thenComparing(Product::getId);
            case "newest" -> Comparator.comparing(Product::getId, Comparator.nullsLast(Long::compareTo)).reversed();
            case "name_asc" -> Comparator.comparing((Product product) -> resolveLocalizedName(product, language), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Product::getId);
            default -> Comparator.comparing(Product::getId, Comparator.nullsLast(Long::compareTo));
        };
    }

    private String resolveLocalizedName(Product product, String language) {
        LocalizedContent localized = resolveLocalizedContent(product.getTranslations(), language, product.getName(), product.getDescription());
        if (localized.getName() != null) {
            return localized.getName();
        }
        return "";
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private void applyRequest(Product product, ProductRequest request) {
        product.setSku(request.getSku());
        product.setModel(request.getModel());

        Map<String, LocalizedContent> normalizedTranslations = normalizeLocalizedContent(
            request.getTranslations(),
            request.getName(),
            request.getDescription(),
            product.getName(),
            product.getDescription(),
            "Product name is required"
        );

        LocalizedContent defaultContent = normalizedTranslations.get(LanguageSupport.DEFAULT_LANGUAGE);
        product.setName(defaultContent.getName());
        product.setDescription(defaultContent.getDescription());
        syncTranslations(product, normalizedTranslations);

        product.setPrice(request.getPrice());
        product.setQuantity(request.getQuantity());
        product.setActive(request.getActive());

        if (request.getImage() != null) {
            product.setImage(request.getImage());
        }
        product.setSeoKeywords(trimToNull(request.getSeoKeywords()));
        syncVariants(product, request.getVariants());

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

    private void syncTranslations(Product product, Map<String, LocalizedContent> localizedContents) {
        Map<String, ProductTranslation> existingByLanguage = product.getTranslations().stream()
            .collect(Collectors.toMap(
                translation -> translation.getLanguageCode().toLowerCase(Locale.ROOT),
                translation -> translation,
                (first, ignored) -> first
            ));

        Set<String> languagesToKeep = new LinkedHashSet<>();

        for (String language : LanguageSupport.SUPPORTED_LANGUAGES) {
            LocalizedContent localizedContent = localizedContents.get(language);
            String name = trimToNull(localizedContent != null ? localizedContent.getName() : null);
            String description = trimToNull(localizedContent != null ? localizedContent.getDescription() : null);
            ProductTranslation translation = existingByLanguage.get(language);

            if (!language.equals(LanguageSupport.DEFAULT_LANGUAGE) && name == null && description == null) {
                if (translation != null) {
                    product.getTranslations().remove(translation);
                }
                continue;
            }

            if (translation == null) {
                translation = new ProductTranslation();
                translation.setProduct(product);
                translation.setLanguageCode(language);
                product.getTranslations().add(translation);
                existingByLanguage.put(language, translation);
            }

            translation.setName(language.equals(LanguageSupport.DEFAULT_LANGUAGE)
                ? firstNonBlank(name, product.getName())
                : (name != null ? name : ""));
            translation.setDescription(description);
            languagesToKeep.add(language);
        }

        product.getTranslations().removeIf(translation ->
            !languagesToKeep.contains(translation.getLanguageCode().toLowerCase(Locale.ROOT)));
    }

    private ProductResponse toResponse(Product product, String language) {
        ProductImageResponse coverImage = productImageService.resolveCoverImage(product.getId());

        LocalizedContent localized = resolveLocalizedContent(product.getTranslations(), language, product.getName(), product.getDescription());

        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setSku(product.getSku());
        response.setModel(product.getModel());
        response.setName(localized.getName());
        response.setDescription(localized.getDescription());
        response.setPrice(product.getPrice());
        response.setQuantity(product.getQuantity());
        response.setActive(product.getActive());

        String coverUrl = resolveDisplayImageUrl(coverImage, product.getImage());
        response.setImage(coverUrl);
        response.setSeoKeywords(product.getSeoKeywords());
        response.setCoverImageUrl(coverUrl);
        response.setGalleryImages(productImageService.resolveGalleryImages(product.getId()));

        response.setManufacturerId(product.getManufacturer() != null ? product.getManufacturer().getId() : null);
        response.setCategoryIds(product.getCategories().stream().map(Category::getId).collect(Collectors.toSet()));
        response.setCreatedAt(product.getCreatedAt());
        response.setUpdatedAt(product.getUpdatedAt());
        response.setTranslations(toLocalizedContentMap(product.getTranslations(), product.getName(), product.getDescription()));
        response.setVariants(product.getVariants().stream()
            .sorted(Comparator.comparing(ProductVariant::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(ProductVariant::getId, Comparator.nullsLast(Long::compareTo)))
            .map(this::toVariantResponse)
            .toList());
        return response;
    }

    private void syncVariants(Product product, List<ProductVariantRequest> requests) {
        Map<String, ProductVariant> existingByKey = product.getVariants().stream()
            .collect(Collectors.toMap(ProductVariant::getVariantKey, Function.identity(), (first, ignored) -> first, LinkedHashMap::new));

        List<ProductVariantRequest> normalizedRequests = requests != null ? requests : List.of();
        Set<String> seenKeys = new LinkedHashSet<>();
        List<ProductVariant> synced = new ArrayList<>();
        int index = 0;

        for (ProductVariantRequest request : normalizedRequests) {
            if (request == null) {
                continue;
            }
            String variantKey = trimToNull(request.getVariantKey());
            String optionSummary = trimToNull(request.getOptionSummary());
            if (variantKey == null) {
                continue;
            }
            if (!VARIANT_KEY_PATTERN.matcher(variantKey).matches()) {
                throw new BadRequestException("Invalid variant key: " + variantKey);
            }
            if (!seenKeys.add(variantKey)) {
                throw new BadRequestException("Duplicate variant key: " + variantKey);
            }

            ProductVariant variant = existingByKey.getOrDefault(variantKey, new ProductVariant());
            variant.setProduct(product);
            variant.setVariantKey(variantKey);
            variant.setSku(trimToNull(request.getSku()));
            variant.setDisplayName(firstNonBlank(trimToNull(request.getDisplayName()), optionSummary, variantKey));
            variant.setOptionSummary(optionSummary);
            variant.setImageUrl(trimToNull(request.getImageUrl()));
            variant.setPriceOverride(request.getPriceOverride());
            variant.setQuantity(request.getQuantity() != null ? Math.max(0, request.getQuantity()) : 0);
            variant.setActive(request.getActive() == null || request.getActive());
            variant.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : index);
            synced.add(variant);
            index++;
        }

        product.getVariants().clear();
        product.getVariants().addAll(synced);
    }

    private ProductVariantResponse toVariantResponse(ProductVariant variant) {
        ProductVariantResponse response = new ProductVariantResponse();
        response.setId(variant.getId());
        response.setVariantKey(variant.getVariantKey());
        response.setSku(variant.getSku());
        response.setDisplayName(variant.getDisplayName());
        response.setOptionSummary(variant.getOptionSummary());
        response.setImageUrl(variant.getImageUrl());
        response.setPriceOverride(variant.getPriceOverride());
        response.setQuantity(variant.getQuantity());
        response.setActive(variant.getActive());
        response.setSortOrder(variant.getSortOrder());
        return response;
    }

    private String resolveDisplayImageUrl(ProductImageResponse coverImage, String legacyImageUrl) {
        if (coverImage != null && coverImage.getUrl() != null && !coverImage.getUrl().isBlank()) {
            return coverImage.getUrl();
        }
        if (legacyImageUrl == null || legacyImageUrl.isBlank()) {
            return null;
        }
        if (legacyImageUrl.startsWith("http://") || legacyImageUrl.startsWith("https://")) {
            return legacyImageUrl;
        }
        if (legacyImageUrl.startsWith("/api/catalog/media/")) {
            return legacyImageUrl;
        }
        return null;
    }

    private Map<String, LocalizedContent> toLocalizedContentMap(List<ProductTranslation> translations, String fallbackName, String fallbackDescription) {
        Map<String, LocalizedContent> map = new LinkedHashMap<>();
        Map<String, ProductTranslation> byLanguage = translations.stream()
            .collect(Collectors.toMap(
                translation -> translation.getLanguageCode().toLowerCase(Locale.ROOT),
                translation -> translation,
                (first, ignored) -> first
            ));

        for (String language : LanguageSupport.SUPPORTED_LANGUAGES) {
            ProductTranslation translation = byLanguage.get(language);
            LocalizedContent content = new LocalizedContent();
            String translatedName = trimToNull(translation != null ? translation.getName() : null);
            String translatedDescription = trimToNull(translation != null ? translation.getDescription() : null);

            if (language.equals(LanguageSupport.DEFAULT_LANGUAGE)) {
                translatedName = firstNonBlank(translatedName, fallbackName);
                translatedDescription = firstNonBlank(translatedDescription, fallbackDescription);
            }

            content.setName(translatedName);
            content.setDescription(translatedDescription);
            map.put(language, content);
        }
        return map;
    }

    private LocalizedContent resolveLocalizedContent(
        List<ProductTranslation> translations,
        String language,
        String fallbackName,
        String fallbackDescription
    ) {
        String resolvedLanguage = LanguageSupport.normalizeLanguage(language);
        if (resolvedLanguage == null) {
            resolvedLanguage = LanguageSupport.DEFAULT_LANGUAGE;
        }

        LocalizedContent requestedContent = findLocalizedContent(translations, resolvedLanguage);
        LocalizedContent defaultContent = findLocalizedContent(translations, LanguageSupport.DEFAULT_LANGUAGE);
        LocalizedContent localized = new LocalizedContent();
        localized.setName(firstNonBlank(
            requestedContent != null ? requestedContent.getName() : null,
            defaultContent != null ? defaultContent.getName() : null,
            fallbackName
        ));
        localized.setDescription(firstNonBlank(
            requestedContent != null ? requestedContent.getDescription() : null,
            defaultContent != null ? defaultContent.getDescription() : null,
            fallbackDescription
        ));
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
            if (language.equals(LanguageSupport.DEFAULT_LANGUAGE)) {
                content.setName(defaultName);
                content.setDescription(defaultDescription);
            } else {
                content.setName(extractValue(requested, language, true));
                content.setDescription(extractValue(requested, language, false));
            }
            normalized.put(language, content);
        }

        return normalized;
    }

    private Map<String, LocalizedContent> normalizeTranslationPayload(Map<String, LocalizedContent> requested) {
        Map<String, LocalizedContent> normalized = new LinkedHashMap<>();
        for (String language : LanguageSupport.SUPPORTED_LANGUAGES) {
            LocalizedContent source = requested != null ? requested.get(language) : null;
            LocalizedContent content = new LocalizedContent();
            content.setName(trimToNull(source != null ? source.getName() : null));
            content.setDescription(trimToNull(source != null ? source.getDescription() : null));
            normalized.put(language, content);
        }
        return normalized;
    }

    private LocalizedContent findLocalizedContent(List<ProductTranslation> translations, String language) {
        if (translations == null) {
            return null;
        }

        String normalizedLanguage = LanguageSupport.normalizeLanguage(language);
        if (normalizedLanguage == null) {
            return null;
        }

        for (ProductTranslation translation : translations) {
            if (normalizedLanguage.equals(LanguageSupport.normalizeLanguage(translation.getLanguageCode()))) {
                LocalizedContent content = new LocalizedContent();
                content.setName(trimToNull(translation.getName()));
                content.setDescription(trimToNull(translation.getDescription()));
                return content;
            }
        }

        return null;
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

    private boolean isBlank(String value) {
        return trimToNull(value) == null;
    }
}
