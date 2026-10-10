package io.petcare.backend.ai.agent;

public interface PetAgentService {

    AiChatResult chat(AiChatCommand command);
}
