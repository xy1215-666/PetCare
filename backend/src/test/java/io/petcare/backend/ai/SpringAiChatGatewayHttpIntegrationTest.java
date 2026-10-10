package io.petcare.backend.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SpringAiChatGatewayHttpIntegrationTest {

    private static final String MODEL_REPLY = "我目前没有获得小白的真实记录，无法判断。";
    private static final String OPENAI_RESPONSE = """
            {"id":"chatcmpl-local-test","object":"chat.completion","created":1750000000,
             "model":"qwen3.7-plus","choices":[{"index":0,"message":{"role":"assistant","content":"我目前没有获得小白的真实记录，无法判断。"},"finish_reason":"stop"}],
             "usage":{"prompt_tokens":10,"completion_tokens":20,"total_tokens":30}}
            """;
    private static final AtomicReference<String> lastRequestBody = new AtomicReference<>();
    private static final AtomicReference<String> lastRequestPath = new AtomicReference<>();
    private static final AtomicReference<String> lastAuthorization = new AtomicReference<>();
    private static final AtomicInteger requestCount = new AtomicInteger();
    private static final AtomicInteger responseStatus = new AtomicInteger(200);
    private static final AtomicReference<String> responseBody = new AtomicReference<>(OPENAI_RESPONSE);
    private static final HttpServer OPENAI_COMPATIBLE_SERVER = startServer();

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void resetLocalProvider() {
        requestCount.set(0);
        lastRequestBody.set(null);
        lastRequestPath.set(null);
        lastAuthorization.set(null);
        responseStatus.set(200);
        responseBody.set(OPENAI_RESPONSE);
    }

    @DynamicPropertySource
    static void providerProperties(DynamicPropertyRegistry registry) {
        String baseUrl = "http://127.0.0.1:" + OPENAI_COMPATIBLE_SERVER.getAddress().getPort() + "/v1";
        registry.add("PETCARE_AI_ENABLED", () -> "true");
        registry.add("PETCARE_AI_PROVIDER", () -> "openai-compatible");
        registry.add("PETCARE_AI_MODEL", () -> "qwen3.7-plus");
        registry.add("PETCARE_AI_BASE_URL", () -> baseUrl);
        registry.add("DASHSCOPE_API_KEY", () -> "test-only-key");
        registry.add("spring.ai.openai.base-url", () -> baseUrl);
        registry.add("spring.ai.openai.api-key", () -> "test-only-key");
        registry.add("spring.ai.openai.chat.model", () -> "qwen3.7-plus");
        registry.add("spring.ai.openai.chat.temperature", () -> "0.3");
        registry.add("spring.ai.openai.chat.extra-body.enable_thinking", () -> "false");
        registry.add("spring.ai.openai.timeout", () -> "5s");
        registry.add("spring.ai.openai.max-retries", () -> "0");
        registry.add("spring.ai.retry.max-attempts", () -> "1");
    }

    @AfterAll
    static void stopServer() {
        OPENAI_COMPATIBLE_SERVER.stop(0);
    }

    @Test
    void jwtChatSendsToolSchemaWithoutTrustedUserContextToTheProvider() throws Exception {
        mockMvc.perform(post("/api/ai/chat")
                        .with(jwt().jwt(token -> token.subject("42")))
                        .contentType("application/json")
                        .content("{\"message\":\"小白最近状态怎么样？\",\"userId\":999}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.content").value(MODEL_REPLY));

        JsonNode request = objectMapper.readTree(lastRequestBody.get());
        assertThat(requestCount.get()).isEqualTo(1);
        assertThat(lastRequestPath.get()).isEqualTo("/v1/chat/completions");
        assertThat(lastAuthorization.get()).isEqualTo("Bearer test-only-key");
        assertThat(request.path("model").asText()).isEqualTo("qwen3.7-plus");
        assertThat(request.path("temperature").asDouble()).isEqualTo(0.3);
        assertThat(request.path("enable_thinking").asBoolean()).isFalse();
        assertThat(request.path("tools")).hasSize(1);
        JsonNode profileTool = request.path("tools").get(0).path("function");
        assertThat(profileTool.path("name").asText()).isEqualTo("getPetProfile");
        assertThat(profileTool.path("parameters").path("properties").has("petName")).isTrue();
        assertThat(profileTool.path("parameters").path("properties").has("userId")).isFalse();
        assertThat(request.toString()).contains("暂时无法读取相关记录", "小白最近状态怎么样？");
        assertThat(request.toString()).doesNotContain("userId", "42", "999", "test-only-key");
    }

    @Test
    void provider429IsNotRetriedAndMapsToSanitized503() throws Exception {
        responseStatus.set(429);
        responseBody.set("{\"error\":{\"message\":\"upstream secret detail\"}}");

        mockMvc.perform(post("/api/ai/chat")
                        .with(jwt().jwt(token -> token.subject("42")))
                        .contentType("application/json")
                        .content("{\"message\":\"你好\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(503))
                .andExpect(jsonPath("$.message").value("智能助手暂时不可用，请稍后再试"));

        assertThat(requestCount.get()).isEqualTo(1);
        assertThat(lastAuthorization.get()).isEqualTo("Bearer test-only-key");
    }

    private static HttpServer startServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                lastRequestPath.set(exchange.getRequestURI().getPath());
                lastAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
                lastRequestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                requestCount.incrementAndGet();

                byte[] body = SpringAiChatGatewayHttpIntegrationTest.responseBody.get()
                        .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(SpringAiChatGatewayHttpIntegrationTest.responseStatus.get(), body.length);
                try (var output = exchange.getResponseBody()) {
                    output.write(body);
                }
            });
            server.start();
            return server;
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
