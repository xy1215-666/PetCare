package io.petcare.backend.ai;

import io.petcare.backend.ai.agent.AiChatCommand;
import io.petcare.backend.ai.agent.DefaultPetAgentService;
import io.petcare.backend.ai.config.PetCareAiProperties;
import io.petcare.backend.ai.prompt.PetAgentSystemPrompt;
import io.petcare.backend.ai.service.AiChatGateway;
import io.petcare.backend.ai.service.SpringAiChatGateway;
import io.petcare.backend.ai.tool.PetProfileTool;
import io.petcare.backend.pet.CreatePetRequest;
import io.petcare.backend.pet.PetGender;
import io.petcare.backend.pet.PetService;
import io.petcare.backend.pet.PetType;
import io.petcare.backend.pet.PetVO;
import io.petcare.backend.support.TestCredentials;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class PetProfileToolCallingIntegrationTest {

    @Container
    private static final MySQLContainer MYSQL = new MySQLContainer(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("petcare")
            .withUsername("petcare")
            .withPassword(TestCredentials.newPassword());

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

    @org.springframework.beans.factory.annotation.Autowired
    private PetService petService;

    @org.springframework.beans.factory.annotation.Autowired
    private PetProfileTool petProfileTool;

    @org.springframework.beans.factory.annotation.Autowired
    private PetAgentSystemPrompt systemPrompt;

    @org.springframework.beans.factory.annotation.Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void fakeModelDrivesAgentToolServiceDatabaseChainAndRespectsTrustedUserContext() {
        long currentUserId = createUser();
        long otherUserId = createUser();
        PetVO ownedPet = createPet(currentUserId, "小白", PetType.CAT, "英短");
        createPet(otherUserId, "小白", PetType.DOG, "比熊");
        createPet(currentUserId, "团团", PetType.CAT, "布偶");
        createPet(currentUserId, "团团", PetType.DOG, "柴犬");

        ToolCallback[] toolCallbacks = ToolCallbacks.from(petProfileTool);
        assertThat(toolCallbacks).hasSize(1);
        assertThat(toolCallbacks[0].getToolDefinition().name()).isEqualTo("getPetProfile");
        assertThat(toolCallbacks[0].getToolDefinition().inputSchema())
                .contains("petName")
                .doesNotContain("userId", "currentUserId");

        FakeToolCallingChatModel uniquePetModel = new FakeToolCallingChatModel(
                "小白是一只英短猫。", "小白");
        var uniquePetAnswer = callAgent(agent(uniquePetModel), uniquePetModel, currentUserId, "小白是谁？");
        String uniqueToolResult = uniquePetModel.toolResult.get();
        assertThat(uniquePetAnswer.content()).isEqualTo("小白是一只英短猫。");
        assertThat(uniquePetModel.calls.get()).isEqualTo(2);
        assertThat(uniquePetModel.systemMessage.get()).contains("必须优先调用宠物档案工具");
        assertThat(uniqueToolResult)
                .contains("\"status\":\"FOUND\"")
                .contains("\"petId\":" + ownedPet.id())
                .contains("\"breed\":\"英短\"")
                .doesNotContain("userId", "deleted", "createTime", "updateTime", "private remark");
        assertThat(uniqueToolResult).doesNotContain("比熊");

        FakeToolCallingChatModel missingPetModel = new FakeToolCallingChatModel(
                "系统里没有找到这只宠物。", "不存在的宠物");
        agent(missingPetModel).chat(new AiChatCommand(currentUserId, "我的宠物里有不存在的宠物吗？"));
        assertThat(missingPetModel.toolResult.get())
                .contains("\"status\":\"NOT_FOUND\"")
                .contains("未找到名为“\u4e0d\u5b58\u5728\u7684\u5ba0\u7269”的宠物");

        FakeToolCallingChatModel duplicatePetModel = new FakeToolCallingChatModel(
                "有两只团团，请告诉我你指的是哪一只。", "团团");
        agent(duplicatePetModel).chat(new AiChatCommand(currentUserId, "团团的档案是什么？"));
        assertThat(duplicatePetModel.toolResult.get())
                .contains("\"status\":\"AMBIGUOUS\"")
                .contains("\"matches\":[")
                .contains("布偶")
                .contains("柴犬")
                .contains("请用户进一步确认具体宠物");
    }

    private DefaultPetAgentService agent(FakeToolCallingChatModel model) {
        ChatClient chatClient = ChatClient.builder(model)
                .defaultAdvisors(ToolCallingAdvisor.builder().build())
                .build();
        PetCareAiProperties properties = new PetCareAiProperties();
        properties.setEnabled(true);
        AiChatGateway gateway = new SpringAiChatGateway(
                chatClient, petProfileTool, properties.getModel(), properties.getTemperature());
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("fakeToolCallingGateway", gateway);
        ObjectProvider<AiChatGateway> provider = beanFactory.getBeanProvider(AiChatGateway.class);
        return new DefaultPetAgentService(properties, systemPrompt, provider);
    }

    private io.petcare.backend.ai.agent.AiChatResult callAgent(
            DefaultPetAgentService agent,
            FakeToolCallingChatModel model,
            long userId,
            String message) {
        try {
            return agent.chat(new AiChatCommand(userId, message));
        } catch (RuntimeException exception) {
            throw new AssertionError("Fake model call count: " + model.calls.get()
                    + ", options type: " + model.optionsType.get(), exception);
        }
    }

    private long createUser() {
        String username = "tool_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        jdbcTemplate.update("""
                INSERT INTO app_user (username, password_hash, user_role, status, deleted)
                VALUES (?, 'test-hash', 'USER', 'ACTIVE', 0)
                """, username);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM app_user WHERE username = ?", Long.class, username);
    }

    private PetVO createPet(long userId, String name, PetType type, String breed) {
        return petService.create(userId, new CreatePetRequest(
                name,
                type,
                breed,
                PetGender.FEMALE,
                LocalDate.of(2022, 5, 1),
                new BigDecimal("4.250"),
                true,
                null,
                "private remark must not reach the model"));
    }

    private static final class FakeToolCallingChatModel implements ChatModel {

        private final String finalAnswer;
        private final String petName;
        private final AtomicInteger calls = new AtomicInteger();
        private final AtomicReference<String> toolResult = new AtomicReference<>();
        private final AtomicReference<String> systemMessage = new AtomicReference<>();
        private final AtomicReference<String> optionsType = new AtomicReference<>();

        private FakeToolCallingChatModel(String finalAnswer, String petName) {
            this.finalAnswer = finalAnswer;
            this.petName = petName;
        }

        @Override
        public ChatOptions getOptions() {
            return ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            systemMessage.compareAndSet(null, prompt.getSystemMessage().getText());
            optionsType.compareAndSet(null, prompt.getOptions() == null
                    ? "null"
                    : prompt.getOptions().getClass().getName());
            if (calls.getAndIncrement() == 0) {
                return new ChatResponse(List.of(new Generation(AssistantMessage.builder()
                        .toolCalls(List.of(new AssistantMessage.ToolCall(
                                "tool-call-1",
                                "function",
                                "getPetProfile",
                                "{\"petName\":\"" + petName + "\"}")))
                        .build())));
            }

            ToolResponseMessage response = prompt.getInstructions().stream()
                    .filter(ToolResponseMessage.class::isInstance)
                    .map(ToolResponseMessage.class::cast)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("ToolCallingAdvisor did not return tool results to the model"));
            assertThat(response.getResponses()).hasSize(1);
            toolResult.set(response.getResponses().getFirst().responseData());
            return new ChatResponse(List.of(new Generation(new AssistantMessage(finalAnswer))));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }
    }
}
