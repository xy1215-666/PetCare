package io.petcare.backend.ai.agent;

import io.petcare.backend.ai.config.PetCareAiProperties;
import io.petcare.backend.ai.exception.AiProviderException;
import io.petcare.backend.ai.exception.AiUnavailableException;
import io.petcare.backend.ai.prompt.PetAgentSystemPrompt;
import io.petcare.backend.ai.service.AiChatGateway;
import org.springframework.beans.factory.ObjectProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DefaultPetAgentService implements PetAgentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPetAgentService.class);

    private final PetCareAiProperties properties;
    private final PetAgentSystemPrompt systemPrompt;
    private final ObjectProvider<AiChatGateway> gatewayProvider;

    public DefaultPetAgentService(
            PetCareAiProperties properties,
            PetAgentSystemPrompt systemPrompt,
            ObjectProvider<AiChatGateway> gatewayProvider) {
        this.properties = properties;
        this.systemPrompt = systemPrompt;
        this.gatewayProvider = gatewayProvider;
    }

    @Override
    public AiChatResult chat(AiChatCommand command) {
        if (!properties.isEnabled()) {
            throw new AiUnavailableException();
        }

        AiChatGateway gateway = gatewayProvider.getIfUnique();
        if (gateway == null) {
            throw new AiProviderException();
        }

        long startedAt = System.nanoTime();
        try {
            String content = gateway.complete(systemPrompt.content(), command.message(), command.userId());
            if (content == null || content.isBlank()) {
                throw new AiProviderException();
            }
            LOGGER.info("AI chat provider={} model={} latencyMs={} success=true",
                    properties.getProvider(), properties.getModel(), elapsedMillis(startedAt));
            return new AiChatResult(content.trim());
        } catch (AiProviderException exception) {
            LOGGER.warn("AI chat provider={} model={} latencyMs={} success=false",
                    properties.getProvider(), properties.getModel(), elapsedMillis(startedAt));
            throw exception;
        } catch (RuntimeException exception) {
            LOGGER.warn("AI chat provider={} model={} latencyMs={} success=false",
                    properties.getProvider(), properties.getModel(), elapsedMillis(startedAt));
            throw new AiProviderException(exception);
        }
    }

    private static long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
