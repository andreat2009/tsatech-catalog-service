package com.newproject.catalog.service;

import com.newproject.catalog.config.CatalogTranslationProperties;
import com.newproject.catalog.dto.LocalizedContent;
import com.newproject.catalog.service.translation.NoopTranslationProvider;
import com.newproject.catalog.service.translation.OpenAiTranslationProvider;
import com.newproject.catalog.service.translation.TranslationResult;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class CatalogTranslationService {
    private final CatalogTranslationProperties properties;
    private final OpenAiTranslationProvider openAiTranslationProvider;
    private final NoopTranslationProvider noopTranslationProvider;

    public CatalogTranslationService(
        CatalogTranslationProperties properties,
        OpenAiTranslationProvider openAiTranslationProvider,
        NoopTranslationProvider noopTranslationProvider
    ) {
        this.properties = properties;
        this.openAiTranslationProvider = openAiTranslationProvider;
        this.noopTranslationProvider = noopTranslationProvider;
    }

    public TranslationResult translateProductContent(String sourceLanguage, LocalizedContent sourceContent, Set<String> targetLanguages) {
        if (!properties.isEnabled()) {
            TranslationResult result = noopTranslationProvider.translateProductContent(sourceLanguage, sourceContent, targetLanguages);
            result.getWarnings().add("Translation disabled");
            return result;
        }

        String provider = properties.getProvider() != null ? properties.getProvider().trim().toLowerCase() : "";
        if ("openai".equals(provider)) {
            return openAiTranslationProvider.translateProductContent(sourceLanguage, sourceContent, targetLanguages);
        }

        TranslationResult result = noopTranslationProvider.translateProductContent(sourceLanguage, sourceContent, targetLanguages);
        result.getWarnings().add("Unsupported translation provider: " + provider);
        return result;
    }
}
