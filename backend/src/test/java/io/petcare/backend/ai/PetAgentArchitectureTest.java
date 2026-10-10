package io.petcare.backend.ai;

import io.petcare.backend.ai.agent.AiChatCommand;
import io.petcare.backend.ai.agent.AiChatResult;
import io.petcare.backend.ai.agent.DefaultPetAgentService;
import io.petcare.backend.ai.AiChatController;
import io.petcare.backend.ai.agent.PetAgentService;
import io.petcare.backend.ai.config.PetCareAiProperties;
import io.petcare.backend.ai.config.AiProviderConfigurationValidator;
import io.petcare.backend.ai.dto.AiChatRequest;
import io.petcare.backend.ai.exception.AiExceptionHandler;
import io.petcare.backend.ai.exception.AiProviderException;
import io.petcare.backend.ai.exception.AiUnavailableException;
import io.petcare.backend.ai.prompt.PetAgentSystemPrompt;
import io.petcare.backend.ai.service.AiChatGateway;
import io.petcare.backend.ai.service.SpringAiChatGateway;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PetAgentArchitectureTest {

    @Test
    void fakeChatModelReturnsFixedResponseThroughPetAgentServiceAndSpringAiChatClient() {
        PetCareAiProperties properties = enabledProperties();
        PetAgentSystemPrompt systemPrompt = loadPrompt();
        FixedChatModel model = new FixedChatModel("测试回复");
        AiChatGateway gateway = new SpringAiChatGateway(ChatClient.builder(model).build());
        DefaultPetAgentService service = service(properties, systemPrompt, gateway);

        AiChatResult result = service.chat(new AiChatCommand(42, "你好"));

        assertThat(result.content()).isEqualTo("测试回复");
        assertThat(model.lastPrompt.get().getSystemMessage().getText())
                .contains("不得编造系统中不存在的数据")
                .contains("当前没有获得这只宠物的真实记录");
        assertThat(model.lastPrompt.get().getUserMessage().getText()).isEqualTo("你好");
    }

    @Test
    void disabledAiFailsWithSafeUnavailableMessageWithoutCallingGateway() {
        PetCareAiProperties properties = new PetCareAiProperties();
        AtomicReference<Boolean> called = new AtomicReference<>(false);
        AiChatGateway gateway = (system, user) -> {
            called.set(true);
            return "should not be called";
        };

        assertThatThrownBy(() -> service(properties, loadPrompt(), gateway)
                .chat(new AiChatCommand(42, "你好")))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessage("智能助手当前未启用");
        assertThat(called.get()).isFalse();
    }

    @Test
    void enabledAiWithoutGatewayFailsClearlyInsteadOfReturningFakeText() {
        assertThatThrownBy(() -> service(enabledProperties(), loadPrompt(), null)
                .chat(new AiChatCommand(42, "你好")))
                .isInstanceOf(AiProviderException.class)
                .hasMessage("智能助手暂时不可用，请稍后再试");
    }

    @Test
    void providerFailureIsWrappedWithoutLeakingInternalDetails() {
        AiChatGateway gateway = (system, user) -> {
            throw new IllegalStateException("secret provider detail");
        };

        assertThatThrownBy(() -> service(enabledProperties(), loadPrompt(), gateway)
                .chat(new AiChatCommand(42, "你好")))
                .isInstanceOf(AiProviderException.class)
                .hasMessage("智能助手暂时不可用，请稍后再试")
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void emptyModelResponseIsNotReturnedAsSuccessfulAnswer() {
        AiChatGateway gateway = (system, user) -> "  ";

        assertThatThrownBy(() -> service(enabledProperties(), loadPrompt(), gateway)
                .chat(new AiChatCommand(42, "你好")))
                .isInstanceOf(AiProviderException.class);
    }

    @Test
    void systemPromptLoadsFromClasspathAndMissingOrEmptyPromptFailsClearly() {
        PetAgentSystemPrompt prompt = loadPrompt();
        assertThat(prompt.content()).contains("不进行确定性疾病诊断");

        assertThatThrownBy(() -> new PetAgentSystemPrompt(new ClassPathResource("prompts/missing-prompt.txt")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("system prompt is missing");
        assertThatThrownBy(() -> new PetAgentSystemPrompt(
                new ByteArrayResource("   ".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("system prompt is empty");
    }

    @Test
    void aiErrorsMapTo503WithFriendlyApiResponses() {
        AiExceptionHandler handler = new AiExceptionHandler();

        ResponseEntity<?> unavailable = handler.handleUnavailable(new AiUnavailableException());
        ResponseEntity<?> providerFailure = handler.handleProviderFailure(new AiProviderException(
                new IllegalStateException("internal detail")));

        assertThat(unavailable.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(providerFailure.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(unavailable.getBody()).hasToString("ApiResponse[code=503, message=智能助手当前未启用, data=null]");
        assertThat(providerFailure.getBody()).hasToString(
                "ApiResponse[code=503, message=智能助手暂时不可用，请稍后再试, data=null]");
        assertThat(providerFailure.getBody().toString()).doesNotContain("internal detail");
    }

    @Test
    void enabledProviderRequiresAnEndpointAndApiKeyWithClearConfigurationErrors() {
        PetCareAiProperties properties = enabledProperties();
        assertThatThrownBy(() -> new AiProviderConfigurationValidator(properties, ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PETCARE_AI_BASE_URL");

        properties.setBaseUrl("https://workspace.cn-beijing.maas.aliyuncs.com/compatible-mode/v1");
        assertThatThrownBy(() -> new AiProviderConfigurationValidator(properties, ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DASHSCOPE_API_KEY");
    }

    @Test
    void controllerUsesAuthenticatedJwtSubjectAndRequestDoesNotExposeUserId() throws Exception {
        AtomicReference<AiChatCommand> receivedCommand = new AtomicReference<>();
        PetAgentService service = command -> {
            receivedCommand.set(command);
            return new AiChatResult("测试回复");
        };
        AiChatController controller = new AiChatController(service);
        AiChatRequest request = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue("{\"message\":\"你好\"}", AiChatRequest.class);
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("42")
                .build();

        var response = controller.chat(jwt, request);

        assertThat(receivedCommand.get()).isEqualTo(new AiChatCommand(42, "你好"));
        assertThat(response.data().content()).isEqualTo("测试回复");
    }

    private static DefaultPetAgentService service(
            PetCareAiProperties properties,
            PetAgentSystemPrompt prompt,
            AiChatGateway gateway) {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        if (gateway != null) {
            beanFactory.addBean("testAiChatGateway", gateway);
        }
        ObjectProvider<AiChatGateway> provider = beanFactory.getBeanProvider(AiChatGateway.class);
        return new DefaultPetAgentService(properties, prompt, provider);
    }

    private static PetCareAiProperties enabledProperties() {
        PetCareAiProperties properties = new PetCareAiProperties();
        properties.setEnabled(true);
        return properties;
    }

    private static PetAgentSystemPrompt loadPrompt() {
        return new PetAgentSystemPrompt(new ClassPathResource("prompts/pet-agent-system.txt"));
    }

    private static final class FixedChatModel implements ChatModel {

        private final String response;
        private final AtomicReference<Prompt> lastPrompt = new AtomicReference<>();

        private FixedChatModel(String response) {
            this.response = response;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            lastPrompt.set(prompt);
            return new ChatResponse(List.of(new Generation(new AssistantMessage(response))));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }
    }
}
