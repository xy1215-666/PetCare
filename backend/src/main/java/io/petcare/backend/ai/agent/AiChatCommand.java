package io.petcare.backend.ai.agent;

import java.util.Objects;

/** Internal command. Its userId must come from the authenticated server-side principal. */
public record AiChatCommand(long userId, String message) {

    public AiChatCommand {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        Objects.requireNonNull(message, "message is required");
        if (message.isBlank() || message.length() > 4000) {
            throw new IllegalArgumentException("message must contain 1 to 4000 characters");
        }
    }
}
