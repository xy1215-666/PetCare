package io.petcare.backend.health;

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
class HealthRecordIntegrationTest {

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
    void createsListsGetsUpdatesAndSoftDeletesHealthRecord() throws Exception {
        String token = registerAndLogin("Health_Crud_" + suffix());
        long petId = createPet(token, "HealthPet");
        LocalDate recordDate = LocalDate.now(ZoneId.of("Asia/Shanghai"));

        MvcResult created = mockMvc.perform(post(path(petId))
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(healthJson(recordDate, "NORMAL", false, null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.petId").value(petId))
                .andExpect(jsonPath("$.data.recordDate").value(recordDate.toString()))
                .andExpect(jsonPath("$.data.appetite").value("NORMAL"))
                .andExpect(jsonPath("$.data.abnormal").value(false))
                .andReturn();
        long recordId = extractLong(created.getResponse().getContentAsString(), "\"id\":");

        mockMvc.perform(get(path(petId))
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(recordId));

        mockMvc.perform(get(path(petId) + "/" + recordId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(recordId));

        mockMvc.perform(put(path(petId) + "/" + recordId)
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(healthJson(recordDate, "REDUCED", true, "ate less than usual")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appetite").value("REDUCED"))
                .andExpect(jsonPath("$.data.abnormal").value(true))
                .andExpect(jsonPath("$.data.abnormalDescription").value("ate less than usual"));

        mockMvc.perform(delete(path(petId) + "/" + recordId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(get(path(petId) + "/" + recordId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(path(petId))
                        .header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void rejectsInvalidEnumFutureDateAndDuplicateActiveDate() throws Exception {
        String token = registerAndLogin("Health_Validation_" + suffix());
        long petId = createPet(token, "ValidationPet");
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));

        mockMvc.perform(post(path(petId))
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(healthJson(today, "HUNGRY", false, null)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post(path(petId))
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(healthJson(today.plusDays(1), "NORMAL", false, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(post(path(petId))
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(healthJson(today, "NORMAL", false, null)))
                .andExpect(status().isCreated());

        mockMvc.perform(post(path(petId))
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(healthJson(today, "NORMAL", false, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void rejectsAbnormalRecordWithoutDescriptionAndAllowsDateReuseAfterDelete() throws Exception {
        String token = registerAndLogin("Health_Abnormal_" + suffix());
        long petId = createPet(token, "AbnormalPet");
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));

        mockMvc.perform(post(path(petId))
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(healthJson(today, "NORMAL", true, "   ")))
                .andExpect(status().isBadRequest());

        MvcResult created = mockMvc.perform(post(path(petId))
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(healthJson(today, "NORMAL", false, null)))
                .andExpect(status().isCreated())
                .andReturn();
        long recordId = extractLong(created.getResponse().getContentAsString(), "\"id\":");
        mockMvc.perform(delete(path(petId) + "/" + recordId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(post(path(petId))
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(healthJson(today, "NORMAL", false, null)))
                .andExpect(status().isCreated());
    }

    @Test
    void hidesForeignPetsAndRecordsAcrossHealthOperations() throws Exception {
        String tokenA = registerAndLogin("Health_A_" + suffix());
        String tokenB = registerAndLogin("Health_B_" + suffix());
        long petA = createPet(tokenA, "PetA");
        long petB = createPet(tokenB, "PetB");
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        MvcResult created = mockMvc.perform(post(path(petA))
                        .header("Authorization", bearer(tokenA))
                        .contentType(APPLICATION_JSON)
                        .content(healthJson(today, "NORMAL", false, null)))
                .andExpect(status().isCreated())
                .andReturn();
        long recordId = extractLong(created.getResponse().getContentAsString(), "\"id\":");
        String missingPath = path(petB) + "/" + recordId;
        String foreignPath = path(petA) + "/" + recordId;

        mockMvc.perform(get(path(petA)).header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(path(petA)).header("Authorization", bearer(tokenB))
                        .contentType(APPLICATION_JSON).content(healthJson(today.minusDays(1), "NORMAL", false, null)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(foreignPath).header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());
        mockMvc.perform(put(foreignPath).header("Authorization", bearer(tokenB))
                        .contentType(APPLICATION_JSON).content(healthJson(today, "POOR", false, null)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(foreignPath).header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get(missingPath).header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());
        assertThat(jdbcTemplate.queryForObject("SELECT deleted FROM health_record WHERE id = ?", Integer.class, recordId))
                .isZero();
    }

    @Test
    void healthEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get(path(1))).andExpect(status().isUnauthorized());
        mockMvc.perform(post(path(1)).contentType(APPLICATION_JSON)
                        .content(healthJson(LocalDate.now(ZoneId.of("Asia/Shanghai")), "NORMAL", false, null)))
                .andExpect(status().isUnauthorized());
    }

    private long createPet(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/pets")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"type\":\"CAT\"}"))
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

    private String healthJson(LocalDate date, String appetite, boolean abnormal, String description) {
        String abnormalValue = description == null ? "null" : "\"" + description + "\"";
        return """
                {
                  "recordDate":"%s",
                  "appetite":"%s",
                  "waterIntake":"NORMAL",
                  "energyLevel":"NORMAL",
                  "activityLevel":"NORMAL",
                  "sleepStatus":"NORMAL",
                  "stoolStatus":"NORMAL",
                  "urinationStatus":"NORMAL",
                  "abnormal":%s,
                  "abnormalDescription":%s,
                  "remark":""
                }
                """.formatted(date, appetite, abnormal, abnormalValue);
    }

    private String path(long petId) {
        return "/api/pets/" + petId + "/health-records";
    }

    private String bearer(String token) {
        return "Bearer " + token;
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
