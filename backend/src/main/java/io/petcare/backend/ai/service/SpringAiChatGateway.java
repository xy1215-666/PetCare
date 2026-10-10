package io.petcare.backend.ai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import io.petcare.backend.ai.tool.PetProfileTool;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SpringAiChatGateway implements AiChatGateway {

    private final ChatClient chatClient;
    private final PetProfileTool petProfileTool;
    private final String model;
    private final double temperature;

    public SpringAiChatGateway(ChatClient chatClient) {
        this(chatClient, null, null, 0.3);
    }

    public SpringAiChatGateway(ChatClient chatClient, PetProfileTool petProfileTool) {
        this(chatClient, petProfileTool, null, 0.3);
    }

    public SpringAiChatGateway(
            ChatClient chatClient,
            PetProfileTool petProfileTool,
            String model,
            double temperature) {
        this.chatClient = Objects.requireNonNull(chatClient);
        this.petProfileTool = petProfileTool;
        this.model = model;
        this.temperature = temperature;
    }

    @Override
    public String complete(String systemMessage, String userMessage) {
        return chatClient.prompt()
                .system(systemMessage)
                .user(userMessage)
                .call()
                .content();
    }

    @Override
    public String complete(String systemMessage, String userMessage, long currentUserId) {
        if (petProfileTool == null) {
            return complete(systemMessage, userMessage);
        }

        ToolCallingChatOptions.Builder<?> options = ToolCallingChatOptions.builder()
                .toolCallbacks(ToolCallbacks.from(petProfileTool))
                .toolContext(Map.of(PetProfileTool.CURRENT_USER_ID_CONTEXT_KEY, currentUserId))
                .temperature(temperature);
        if (model != null && !model.isBlank()) {
            options.model(model);
        }
        Prompt prompt = new Prompt(
                List.of(new SystemMessage(systemMessage), new UserMessage(userMessage)),
                options.build());
        return chatClient.prompt(prompt)
                .call()
                .content();
    }
}
