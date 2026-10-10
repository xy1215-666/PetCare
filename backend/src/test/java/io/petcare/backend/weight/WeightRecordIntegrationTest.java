package io.petcare.backend.weight;

import io.petcare.backend.support.TestCredentials;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class WeightRecordIntegrationTest {

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
    void createsListsUpdatesAndDeletesWeightHistory() throws Exception {
        String token = registerAndLogin("Weight_Crud_" + suffix());
        long petId = createPet(token, "WeightPet", null);
        LocalDate today = today();
        MvcResult older = createWeight(token, petId, today.minusDays(2), "4.200");
        long olderId = extractLong(older.getResponse().getContentAsString(), "\"id\":");
        MvcResult newer = createWeight(token, petId, today.minusDays(1), "4.100");
        long newerId = extractLong(newer.getResponse().getContentAsString(), "\"id\":");

        mockMvc.perform(get(path(petId)).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(olderId))
                .andExpect(jsonPath("$.data[0].weight").value(4.2))
                .andExpect(jsonPath("$.data[1].id").value(newerId))
                .andExpect(jsonPath("$.data[1].recordDate").value(today.minusDays(1).toString()));
        assertPetCurrentWeight(token, petId, 4.1);

        mockMvc.perform(put(path(petId) + "/" + newerId)
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(weightJson(today.minusDays(1), "4.050", "evening")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.weight").value(4.05))
                .andExpect(jsonPath("$.data.remark").value("evening"));
        assertPetCurrentWeight(token, petId, 4.05);

        mockMvc.perform(delete(path(petId) + "/" + newerId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        assertPetCurrentWeight(token, petId, 4.2);
        mockMvc.perform(get(path(petId)).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(olderId));
    }

    @Test
    void deletingAndEditingMostRecentRecordRecomputesCurrentWeight() throws Exception {
        String token = registerAndLogin("Weight_Latest_" + suffix());
        long petId = createPet(token, "LatestPet", null);
        LocalDate today = today();
        MvcResult baseline = createWeight(token, petId, today.minusDays(3), "3.900");
        long baselineId = extractLong(baseline.getResponse().getContentAsString(), "\"id\":");
        MvcResult latest = createWeight(token, petId, today.minusDays(1), "4.400");
        long latestId = extractLong(latest.getResponse().getContentAsString(), "\"id\":");
        assertPetCurrentWeight(token, petId, 4.4);

        mockMvc.perform(put(path(petId) + "/" + latestId)
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(weightJson(today.minusDays(4), "4.500", null)))
                .andExpect(status().isOk());
        assertPetCurrentWeight(token, petId, 3.9);

        mockMvc.perform(delete(path(petId) + "/" + baselineId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        assertPetCurrentWeight(token, petId, 4.5);

        mockMvc.perform(delete(path(petId) + "/" + latestId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/pets/" + petId).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.weight").doesNotExist());
    }

    @Test
    void initialPetWeightIsAddedToWeightHistory() throws Exception {
        String token = registerAndLogin("Weight_Initial_" + suffix());
        long petId = createPet(token, "InitialPet", new BigDecimal("5.250"));

        mockMvc.perform(get(path(petId)).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].weight").value(5.25))
                .andExpect(jsonPath("$.data[0].recordDate").value(today().toString()));
        assertPetCurrentWeight(token, petId, 5.25);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT current_weight FROM pet WHERE id = ?", BigDecimal.class, petId))
                .isEqualByComparingTo("5.250");
    }

    @Test
    void rejectsInvalidWeightAndFutureDate() throws Exception {
        String token = registerAndLogin("Weight_Validation_" + suffix());
        long petId = createPet(token, "ValidationPet", null);
        LocalDate today = today();

        for (String invalidWeight : new String[]{"0", "-1", "500.001", "0.0001"}) {
            mockMvc.perform(post(path(petId))
                            .header("Authorization", bearer(token))
                            .contentType(APPLICATION_JSON)
                            .content(weightJson(today, invalidWeight, null)))
                    .andExpect(status().isBadRequest());
        }
        mockMvc.perform(post(path(petId))
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(weightJson(today.plusDays(1), "4.0", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM weight_record WHERE pet_id = ?", Integer.class, petId))
                .isZero();
    }

    @Test
    void hidesForeignPetsAndRequiresAuthentication() throws Exception {
        String tokenA = registerAndLogin("Weight_A_" + suffix());
        String tokenB = registerAndLogin("Weight_B_" + suffix());
        long petA = createPet(tokenA, "PetA", null);
        long petB = createPet(tokenB, "PetB", null);
        MvcResult created = createWeight(tokenA, petA, today().minusDays(1), "4.0");
        long recordId = extractLong(created.getResponse().getContentAsString(), "\"id\":");
        String pathARecord = path(petA) + "/" + recordId;

        mockMvc.perform(get(path(petA)).header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(path(petA)).header("Authorization", bearer(tokenB))
                        .contentType(APPLICATION_JSON)
                        .content(weightJson(today().minusDays(2), "4.1", null)))
                .andExpect(status().isNotFound());
        mockMvc.perform(put(pathARecord).header("Authorization", bearer(tokenB))
                        .contentType(APPLICATION_JSON)
                        .content(weightJson(today().minusDays(1), "3.8", null)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(pathARecord).header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT deleted FROM weight_record WHERE id = ?", Integer.class, recordId))
                .isZero();

        mockMvc.perform(get(path(petA))).andExpect(status().isUnauthorized());
        mockMvc.perform(post(path(petB)).contentType(APPLICATION_JSON)
                        .content(weightJson(today(), "4.0", null)))
                .andExpect(status().isUnauthorized());
    }

    private long createPet(String token, String name, BigDecimal initialWeight) throws Exception {
        String weight = initialWeight == null ? "" : ",\"weight\":" + initialWeight.toPlainString();
        MvcResult result = mockMvc.perform(post("/api/pets")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"type\":\"CAT\"" + weight + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        return extractLong(result.getResponse().getContentAsString(), "\"id\":");
    }

    private MvcResult createWeight(String token, long petId, LocalDate date, String weight) throws Exception {
        return mockMvc.perform(post(path(petId))
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(weightJson(date, weight, null)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private void assertPetCurrentWeight(String token, long petId, double weight) throws Exception {
        mockMvc.perform(get("/api/pets/" + petId).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.weight").value(weight));
    }

    private String registerAndLogin(String username) throws Exception {
        String password = TestCredentials.newPassword();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isCreated());
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

    private String weightJson(LocalDate date, String weight, String remark) {
        String remarkValue = remark == null ? "null" : "\"" + remark + "\"";
        return """
                {"recordDate":"%s","weight":%s,"remark":%s}
                """.formatted(date, weight, remarkValue);
    }

    private String path(long petId) {
        return "/api/pets/" + petId + "/weight-records";
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private LocalDate today() {
        return LocalDate.now(ZoneId.of("Asia/Shanghai"));
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

    private String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}
