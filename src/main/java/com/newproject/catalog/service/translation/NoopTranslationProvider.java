package com.newproject.catalog.service.translation;

import com.newproject.catalog.dto.LocalizedContent;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class NoopTranslationProvider implements TranslationProvider {
    @Override
    public TranslationResult translateProductContent(String sourceLanguage, LocalizedContent sourceContent, Set<String> targetLanguages) {
        return new TranslationResult();
    }
}
