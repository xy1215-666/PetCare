package io.petcare.backend.ai.prompt;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Component
public class PetAgentSystemPrompt {

    private static final String RESOURCE_LOCATION = "classpath:prompts/pet-agent-system.txt";

    private final String content;

    @Autowired
    public PetAgentSystemPrompt(ResourceLoader resourceLoader) {
        this(resourceLoader.getResource(RESOURCE_LOCATION));
    }

    public PetAgentSystemPrompt(Resource resource) {
        Objects.requireNonNull(resource, "prompt resource is required");
        try {
            if (!resource.exists()) {
                throw new IllegalStateException("Required Pet Agent system prompt is missing");
            }
            this.content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (this.content.isBlank()) {
                throw new IllegalStateException("Required Pet Agent system prompt is empty");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load the Pet Agent system prompt", exception);
        }
    }

    public String content() {
        return content;
    }
}
