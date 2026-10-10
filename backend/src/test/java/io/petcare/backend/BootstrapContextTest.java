package io.petcare.backend;

import io.petcare.backend.ai.agent.PetAgentService;
import io.petcare.backend.ai.config.PetCareAiProperties;
import io.petcare.backend.ai.service.AiChatGateway;
import io.petcare.backend.bootstrap.BootstrapProbeMapper;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.ApplicationContext;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BootstrapContextTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${local.server.port}")
    private int localServerPort;

    @Autowired
    private BootstrapProbeMapper probeMapper;

    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private PetCareAiProperties petCareAiProperties;

    @Autowired
    private PetAgentService petAgentService;

    @Test
    void applicationServerAndMyBatisPlusMapperStart() throws Exception {
        assertThat(sqlSessionTemplate).isNotNull();
        assertThat(probeMapper.selectOne()).isEqualTo(1);
        assertThat(petCareAiProperties.isEnabled()).isFalse();
        assertThat(petAgentService).isNotNull();
        assertThat(applicationContext.getBeansOfType(ChatModel.class)).isNotEmpty();
        assertThat(applicationContext.getBeansOfType(AiChatGateway.class)).isEmpty();

        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + localServerPort + "/api/v1/health"))
                .GET()
                .build();
        var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"");
    }

    @Test
    void protectedApiRequiresAnAuthenticatedPrincipal() throws Exception {
        mockMvc.perform(get("/api/v1/not-a-route"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/not-a-route").with(user("m0-test")))
                .andExpect(status().isNotFound());
    }

    @Test
    void bearerApiRequiresAuthenticationWithoutCsrfTokens() throws Exception {
        mockMvc.perform(post("/api/pets"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/pets").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aiChatRequiresJwtAndDisabledAiReturns503WithoutRequiringProviderCredentials() throws Exception {
        mockMvc.perform(post("/api/ai/chat")
                        .contentType("application/json")
                        .content("{\"message\":\"你好\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/ai/chat")
                        .with(jwt().jwt(token -> token.subject("42")))
                        .contentType("application/json")
                        .content("{\"message\":\"你好\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(503))
                .andExpect(jsonPath("$.message").value("智能助手当前未启用"));

        mockMvc.perform(post("/api/ai/chat")
                        .with(jwt().jwt(token -> token.subject("42")))
                        .contentType("application/json")
                        .content("{\"message\":\"你好\",\"userId\":999}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("智能助手当前未启用"));

        mockMvc.perform(post("/api/ai/chat")
                        .with(jwt().jwt(token -> token.subject("42")))
                        .contentType("application/json")
                        .content("{\"message\":\" \"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/ai/chat")
                        .with(jwt().jwt(token -> token.subject("42")))
                        .contentType("application/json")
                        .content("{\"message\":\"" + "x".repeat(4001) + "\"}"))
                .andExpect(status().isBadRequest());
    }
}
