package io.petcare.backend.auth;

import io.petcare.backend.support.TestCredentials;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class RegistrationIntegrationTest {

    private static final AtomicInteger NEXT_PHONE = new AtomicInteger(0);

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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    void registersNormalizedUserAndStoresOnlyPasswordHash() throws Exception {
        String username = "Pet_User_" + randomSuffix();
        String password = TestCredentials.newPassword();
        String email = "  " + randomSuffix() + "@Example.com  ";
        String phone = nextPhone();

        register(username, password, email, phone)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.username").value(username.toLowerCase()))
                .andExpect(jsonPath("$.data.email").value(email.trim().toLowerCase()))
                .andExpect(jsonPath("$.data.phone").value(phone))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        String passwordHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM app_user WHERE username = ?",
                String.class,
                username.toLowerCase());
        assertThat(passwordHash).isNotEqualTo(password);
        assertThat(passwordEncoder.matches(password, passwordHash)).isTrue();
    }

    @Test
    void rejectsUsernameThatNormalizesToAnExistingUsername() throws Exception {
        register("Unique_Pet_" + randomSuffix(), "LongPassphrase!23", null, null)
                .andExpect(status().isCreated());

        String username = "Shared_Pet_" + randomSuffix();
        register(username, "LongPassphrase!23", null, null).andExpect(status().isCreated());
        register("  " + username.toUpperCase() + "  ", "LongPassphrase!23", null, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void rejectsDuplicateOptionalEmail() throws Exception {
        String email = randomSuffix() + "@example.com";
        register("Email_A_" + randomSuffix(), "LongPassphrase!23", email, null)
                .andExpect(status().isCreated());
        register("Email_B_" + randomSuffix(), "LongPassphrase!23", email.toUpperCase(), null)
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsDuplicateOptionalPhone() throws Exception {
        String phone = nextPhone();
        register("Phone_A_" + randomSuffix(), "LongPassphrase!23", null, phone)
                .andExpect(status().isCreated());
        register("Phone_B_" + randomSuffix(), "LongPassphrase!23", null, phone)
                .andExpect(status().isConflict());
    }

    @Test
    void allowsMultipleUsersWithoutOptionalContactFields() throws Exception {
        register("NoContact_A_" + randomSuffix(), "LongPassphrase!23", null, null)
                .andExpect(status().isCreated());
        register("NoContact_B_" + randomSuffix(), "LongPassphrase!23", null, null)
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsPasswordThatDoesNotMeetTheMinimumLength() throws Exception {
        register("Short_Pass_" + randomSuffix(), "short", null, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void loginReturnsSignedBearerTokenWithOnlyRequiredClaims() throws Exception {
        String username = "Login_User_" + randomSuffix();
        String password = TestCredentials.newPassword();
        register(username, password, null, null).andExpect(status().isCreated());

        MvcResult result = login(username.toUpperCase(), password)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(900))
                .andExpect(jsonPath("$.data.user.username").value(username.toLowerCase()))
                .andExpect(jsonPath("$.data.user.role").value("USER"))
                .andExpect(jsonPath("$.data.user.password").doesNotExist())
                .andReturn();

        String token = extractAccessToken(result.getResponse().getContentAsString());
        var decoded = jwtDecoder.decode(token);
        Object userIdClaim = decoded.getClaim("userId");
        assertThat(decoded.getSubject()).isNotBlank();
        assertThat(userIdClaim).isNotNull();
        assertThat(decoded.getClaimAsString("username")).isEqualTo(username.toLowerCase());
        assertThat(decoded.getClaimAsString("role")).isEqualTo("USER");
        assertThat(decoded.getClaims()).doesNotContainKey("password");

        mockMvc.perform(get("/api/v1/not-a-route").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void wrongPasswordReturnsUniformUnauthorizedJson() throws Exception {
        String username = "Wrong_Pass_" + randomSuffix();
        register(username, "LongPassphrase!23", null, null).andExpect(status().isCreated());

        login(username, "IncorrectPass!23")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("username or password is incorrect"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void protectedRouteReturnsJson401WhenTokenIsMissingOrMalformed() throws Exception {
        mockMvc.perform(get("/api/v1/not-a-route"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(get("/api/v1/not-a-route").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void rejectsExpiredAndTamperedJwtTokens() throws Exception {
        String expiredToken = createExpiredToken();
        mockMvc.perform(get("/api/v1/not-a-route").header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        int signatureStart = expiredToken.lastIndexOf('.') + 1;
        char replacement = expiredToken.charAt(signatureStart) == 'A' ? 'B' : 'A';
        String tamperedToken = expiredToken.substring(0, signatureStart)
                + replacement
                + expiredToken.substring(signatureStart + 1);
        mockMvc.perform(get("/api/v1/not-a-route").header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void currentUserComesFromJwtAndIgnoresAClientSuppliedUserId() throws Exception {
        String usernameA = "Current_A_" + randomSuffix();
        String usernameB = "Current_B_" + randomSuffix();
        String password = TestCredentials.newPassword();
        register(usernameA, password, null, null).andExpect(status().isCreated());
        register(usernameB, password, null, null).andExpect(status().isCreated());

        long userIdA = jdbcTemplate.queryForObject(
                "SELECT id FROM app_user WHERE username = ?", Long.class, usernameA.toLowerCase());
        long userIdB = jdbcTemplate.queryForObject(
                "SELECT id FROM app_user WHERE username = ?", Long.class, usernameB.toLowerCase());
        String token = extractAccessToken(login(usernameA, password)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());

        mockMvc.perform(get("/api/users/me")
                        .param("userId", Long.toString(userIdB))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(userIdA))
                .andExpect(jsonPath("$.data.username").value(usernameA.toLowerCase()))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.createTime").isNotEmpty())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    void currentUserRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    private ResultActions register(String username, String password, String email, String phone) throws Exception {
        StringBuilder json = new StringBuilder("{\"username\":\"")
                .append(username)
                .append("\",\"password\":\"")
                .append(password)
                .append('"');
        if (email != null) {
            json.append(",\"email\":\"").append(email).append('"');
        }
        if (phone != null) {
            json.append(",\"phone\":\"").append(phone).append('"');
        }
        json.append('}');

        return mockMvc.perform(post("/api/auth/register")
                .contentType(APPLICATION_JSON)
                .content(json.toString()));
    }

    private ResultActions login(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"));
    }

    private String extractAccessToken(String responseBody) {
        String marker = "\"accessToken\":\"";
        int tokenStart = responseBody.indexOf(marker);
        assertThat(tokenStart).isGreaterThanOrEqualTo(0);
        tokenStart += marker.length();
        int tokenEnd = responseBody.indexOf('"', tokenStart);
        return responseBody.substring(tokenStart, tokenEnd);
    }

    private String createExpiredToken() {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("petcare-api-test")
                .subject("1")
                .issuedAt(now.minusSeconds(300))
                .expiresAt(now.minusSeconds(60))
                .claim("userId", 1L)
                .claim("username", "expired_user")
                .claim("role", "USER")
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(), claims)).getTokenValue();
    }

    private String randomSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    private String nextPhone() {
        return "1" + String.format("%010d", NEXT_PHONE.incrementAndGet());
    }
}
