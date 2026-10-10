package io.petcare.backend.ai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import io.petcare.backend.ai.config.PetCareAiProperties;
import io.petcare.backend.ai.exception.AiUnavailableException;
import io.petcare.backend.ai.tool.PetProfileTool;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "petcare.ai", name = "enabled", havingValue = "true")
public class SpringAiChatGatewayConfiguration {

    @Bean
    @ConditionalOnMissingBean(AiChatGateway.class)
    AiChatGateway springAiChatGateway(
            ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
            PetCareAiProperties properties,
            PetProfileTool petProfileTool) {
        ChatClient.Builder builder = chatClientBuilderProvider.getIfUnique();
        if (builder == null) {
            return (systemMessage, userMessage) -> {
                throw new AiUnavailableException();
            };
        }

        return new SpringAiChatGateway(
                builder.build(), petProfileTool, properties.getModel(), properties.getTemperature());
    }
}
