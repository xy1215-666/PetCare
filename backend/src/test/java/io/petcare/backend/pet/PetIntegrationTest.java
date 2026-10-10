package io.petcare.backend.pet;

import io.petcare.backend.support.TestCredentials;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

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
class PetIntegrationTest {

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

    @Test
    void createPetRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/pets")
                        .contentType(APPLICATION_JSON)
                        .content(validPetJson("Mimi")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void createsPetAndUsesAuthenticatedOwnerInsteadOfClientSuppliedUserId() throws Exception {
        String username = "Pet_Owner_" + randomSuffix();
        String token = registerAndLogin(username);
        long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM app_user WHERE username = ?", Long.class, username.toLowerCase());

        MvcResult result = mockMvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 999999,
                                  "name": "  Mimi  ",
                                  "type": "CAT",
                                  "breed": "  Domestic Shorthair  ",
                                  "gender": "FEMALE",
                                  "birthday": "2023-06-12",
                                  "weight": 4.250,
                                  "neutered": true,
                                  "homeDate": "2023-08-01",
                                  "remark": "  friendly  "
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.name").value("Mimi"))
                .andExpect(jsonPath("$.data.type").value("CAT"))
                .andExpect(jsonPath("$.data.breed").value("Domestic Shorthair"))
                .andExpect(jsonPath("$.data.gender").value("FEMALE"))
                .andExpect(jsonPath("$.data.weight").value(4.25))
                .andExpect(jsonPath("$.data.neutered").value(true))
                .andExpect(jsonPath("$.data.remark").value("friendly"))
                .andExpect(jsonPath("$.data.userId").doesNotExist())
                .andExpect(jsonPath("$.data.createTime").isNotEmpty())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        long petId = extractLong(response, "\"id\":");
        Long storedOwnerId = jdbcTemplate.queryForObject(
                "SELECT user_id FROM pet WHERE id = ?", Long.class, petId);
        assertThat(storedOwnerId).isEqualTo(userId);
        assertThat(storedOwnerId).isNotEqualTo(999999L);
    }

    @Test
    void rejectsUnknownPetTypeAndOutOfRangeWeight() throws Exception {
        String username = "Pet_Validation_" + randomSuffix();
        String token = registerAndLogin(username);

        mockMvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Mimi\",\"type\":\"BIRD\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Mimi\",\"type\":\"CAT\",\"weight\":500.001}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        Integer petCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pet WHERE user_id = ?", Integer.class,
                jdbcTemplate.queryForObject(
                        "SELECT id FROM app_user WHERE username = ?", Long.class, username.toLowerCase()));
        assertThat(petCount).isZero();
    }

    @Test
    void listAndDetailAreScopedToTheAuthenticatedOwner() throws Exception {
        String usernameA = "Pet_A_" + randomSuffix();
        String usernameB = "Pet_B_" + randomSuffix();
        String tokenA = registerAndLogin(usernameA);
        String tokenB = registerAndLogin(usernameB);

        long petIdA = createPet(tokenA, "A_Pet_" + randomSuffix());
        String petNameB = "B_Pet_" + randomSuffix();
        long petIdB = createPet(tokenB, petNameB);

        mockMvc.perform(get("/api/pets")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(petIdB))
                .andExpect(jsonPath("$.data[0].name").value(petNameB));

        mockMvc.perform(get("/api/pets/" + petIdB)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(petIdB))
                .andExpect(jsonPath("$.data.name").value(petNameB));

        MvcResult foreignPetResponse = mockMvc.perform(get("/api/pets/" + petIdA)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andReturn();
        MvcResult missingPetResponse = mockMvc.perform(get("/api/pets/9223372036854775807")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andReturn();
        assertThat(foreignPetResponse.getResponse().getContentAsString())
                .isEqualTo(missingPetResponse.getResponse().getContentAsString());
    }

    @Test
    void listAndDetailRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/pets"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
        mockMvc.perform(get("/api/pets/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    private long createPet(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/pets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(validPetJson(name)))
                .andExpect(status().isCreated())
                .andReturn();
        return extractLong(result.getResponse().getContentAsString(), "\"id\":");
    }

    private String registerAndLogin(String username) throws Exception {
        String password = TestCredentials.newPassword();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isCreated());
        return loginToken(username, password);
    }

    private String loginToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String response = result.getResponse().getContentAsString();
        String marker = "\"accessToken\":\"";
        int start = response.indexOf(marker);
        assertThat(start).isGreaterThanOrEqualTo(0);
        start += marker.length();
        return response.substring(start, response.indexOf('"', start));
    }

    private String validPetJson(String name) {
        return "{\"name\":\"" + name + "\",\"type\":\"CAT\"}";
    }

    private long extractLong(String json, String marker) {
        int start = json.indexOf(marker);
        assertThat(start).isGreaterThanOrEqualTo(0);
        start += marker.length();
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) {
            end++;
        }
        return Long.parseLong(json.substring(start, end));
    }

    private String randomSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}
