package com.newproject.catalog.service.translation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newproject.catalog.config.CatalogTranslationProperties;
import com.newproject.catalog.dto.LocalizedContent;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OpenAiTranslationProvider implements TranslationProvider {
    private static final Logger logger = LoggerFactory.getLogger(OpenAiTranslationProvider.class);

    private final CatalogTranslationProperties properties;
    private final ObjectMapper objectMapper;

    public OpenAiTranslationProvider(CatalogTranslationProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public TranslationResult translateProductContent(String sourceLanguage, LocalizedContent sourceContent, Set<String> targetLanguages) {
        TranslationResult result = new TranslationResult();

        if (targetLanguages == null || targetLanguages.isEmpty()) {
            return result;
        }

        if (!properties.isEnabled()) {
            result.getWarnings().add("Catalog translation is disabled");
            return result;
        }

        if (!"openai".equalsIgnoreCase(trimToNull(properties.getProvider()))) {
            result.getWarnings().add("Translation provider is not OpenAI");
            return result;
        }

        String apiKey = trimToNull(properties.getOpenai().getApiKey());
        if (apiKey == null) {
            result.getWarnings().add("OPENAI_API_KEY missing: translation skipped");
            return result;
        }

        String sourceName = trimToNull(sourceContent != null ? sourceContent.getName() : null);
        if (sourceName == null) {
            result.getWarnings().add("Missing source product name");
            return result;
        }

        try {
            String content = callOpenAi(
                apiKey,
                firstNonBlank(trimToNull(properties.getModel()), "gpt-4o-mini"),
                normalizeBaseUrl(firstNonBlank(trimToNull(properties.getOpenai().getBaseUrl()), "https://api.openai.com/v1")),
                sourceLanguage,
                sourceContent,
                targetLanguages
            );

            Map<String, LocalizedContent> translated = parseTranslations(content, targetLanguages);
            result.setTranslations(translated);
            if (translated.isEmpty()) {
                result.getWarnings().add("OpenAI response parsed but no translations were produced");
            }
        } catch (Exception ex) {
            logger.warn("OpenAI translation failed: {}", ex.getMessage());
            result.getWarnings().add("OpenAI translation failed: " + ex.getMessage());
        }

        return result;
    }

    private String callOpenAi(
        String apiKey,
        String model,
        String baseUrl,
        String sourceLanguage,
        LocalizedContent sourceContent,
        Set<String> targetLanguages
    ) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(Math.max(1000, properties.getTimeoutMs())))
            .build();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("temperature", 0.2);
        payload.put("messages", List.of(
            Map.of(
                "role", "system",
                "content", "You translate ecommerce product content. Return ONLY valid JSON with language codes as keys and fields name, description. Preserve brand names, model codes, SKUs and numeric values exactly."
            ),
            Map.of(
                "role", "user",
                "content", buildUserPrompt(sourceLanguage, sourceContent, targetLanguages)
            )
        ));

        String body = objectMapper.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/chat/completions"))
            .timeout(Duration.ofMillis(Math.max(1000, properties.getTimeoutMs())))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("HTTP " + response.statusCode() + " from OpenAI");
        }

        JsonNode root = objectMapper.readTree(response.body());
        String content = root.path("choices").path(0).path("message").path("content").asText();
        String trimmed = trimToNull(content);
        if (trimmed == null) {
            throw new IOException("Empty OpenAI content");
        }
        return trimmed;
    }

    private Map<String, LocalizedContent> parseTranslations(String content, Set<String> targetLanguages) throws IOException {
        String jsonPayload = extractJsonPayload(content);
        JsonNode root = objectMapper.readTree(jsonPayload);

        JsonNode candidate = root.has("translations") && root.get("translations").isObject()
            ? root.get("translations")
            : root;

        Map<String, LocalizedContent> translations = new LinkedHashMap<>();
        for (String language : new LinkedHashSet<>(targetLanguages)) {
            JsonNode langNode = candidate.get(language);
            if (langNode == null || !langNode.isObject()) {
                continue;
            }

            LocalizedContent localized = new LocalizedContent();
            localized.setName(trimToNull(langNode.path("name").asText(null)));
            localized.setDescription(trimToNull(langNode.path("description").asText(null)));

            if (localized.getName() != null || localized.getDescription() != null) {
                translations.put(language, localized);
            }
        }

        return translations;
    }

    private String extractJsonPayload(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```") && trimmed.contains("\n")) {
            int start = trimmed.indexOf('\n') + 1;
            int end = trimmed.lastIndexOf("```");
            if (end > start) {
                trimmed = trimmed.substring(start, end).trim();
            }
        }

        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1);
        }
        return trimmed;
    }

    private String buildUserPrompt(String sourceLanguage, LocalizedContent sourceContent, Set<String> targetLanguages) {
        String sourceName = firstNonBlank(trimToNull(sourceContent.getName()), "");
        String sourceDescription = firstNonBlank(trimToNull(sourceContent.getDescription()), "");

        return "Source language: " + sourceLanguage + "\n"
            + "Target languages: " + String.join(",", targetLanguages) + "\n"
            + "Product name: " + sourceName + "\n"
            + "Product description: " + sourceDescription + "\n"
            + "Return JSON only, schema: {\"en\":{\"name\":\"...\",\"description\":\"...\"}, ...}";
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
