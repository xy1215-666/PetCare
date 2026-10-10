package io.petcare.backend.ai;

import io.petcare.backend.ai.agent.AiChatCommand;
import io.petcare.backend.ai.agent.PetAgentService;
import io.petcare.backend.ai.dto.AiChatRequest;
import io.petcare.backend.ai.dto.AiChatResponse;
import io.petcare.backend.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private final PetAgentService petAgentService;

    public AiChatController(PetAgentService petAgentService) {
        this.petAgentService = petAgentService;
    }

    @PostMapping("/chat")
    public ApiResponse<AiChatResponse> chat(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AiChatRequest request) {
        long userId = Long.parseLong(jwt.getSubject());
        var result = petAgentService.chat(new AiChatCommand(userId, request.message()));
        return ApiResponse.success(HttpStatus.OK.value(), "success", new AiChatResponse(result.content()));
    }
}
