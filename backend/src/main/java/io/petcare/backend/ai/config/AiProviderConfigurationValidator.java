package io.petcare.backend.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;

@Component
@ConditionalOnProperty(prefix = "petcare.ai", name = "enabled", havingValue = "true")
public class AiProviderConfigurationValidator {

    public AiProviderConfigurationValidator(
            PetCareAiProperties properties,
            @Value("${DASHSCOPE_API_KEY:}") String apiKey) {
        if (!"openai-compatible".equalsIgnoreCase(properties.getProvider())) {
            throw new IllegalStateException("PETCARE_AI_PROVIDER must be openai-compatible");
        }
        if (!StringUtils.hasText(properties.getBaseUrl())) {
            throw new IllegalStateException("PETCARE_AI_ENABLED=true requires PETCARE_AI_BASE_URL");
        }
        URI baseUrl;
        try {
            baseUrl = URI.create(properties.getBaseUrl());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("PETCARE_AI_BASE_URL must be a valid HTTP or HTTPS URL");
        }
        if (!("http".equalsIgnoreCase(baseUrl.getScheme())
                || "https".equalsIgnoreCase(baseUrl.getScheme())) || !StringUtils.hasText(baseUrl.getHost())) {
            throw new IllegalStateException("PETCARE_AI_BASE_URL must be a valid HTTP or HTTPS URL");
        }
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("PETCARE_AI_ENABLED=true requires DASHSCOPE_API_KEY");
        }
        if (!StringUtils.hasText(properties.getModel())) {
            throw new IllegalStateException("PETCARE_AI_ENABLED=true requires PETCARE_AI_MODEL");
        }
    }
}
