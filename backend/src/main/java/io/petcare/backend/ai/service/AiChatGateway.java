package io.petcare.backend.ai.service;

/** Provider-neutral boundary between the Pet Agent and a chat model implementation. */
public interface AiChatGateway {

    String complete(String systemMessage, String userMessage);

    /**
     * Completes a chat turn with the authenticated user's trusted application context.
     * Legacy/test gateways may ignore this context; Spring AI gateways pass it only as ToolContext.
     */
    default String complete(String systemMessage, String userMessage, long currentUserId) {
        return complete(systemMessage, userMessage);
    }
}
