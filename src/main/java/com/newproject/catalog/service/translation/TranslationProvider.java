package com.newproject.catalog.service.translation;

import com.newproject.catalog.dto.LocalizedContent;
import java.util.Set;

public interface TranslationProvider {
    TranslationResult translateProductContent(String sourceLanguage, LocalizedContent sourceContent, Set<String> targetLanguages);
}
