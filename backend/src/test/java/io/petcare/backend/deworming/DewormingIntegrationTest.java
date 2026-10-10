package io.petcare.backend.deworming;

import io.petcare.backend.support.TestCredentials;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
class DewormingIntegrationTest {

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

    @Test
    void createsListsGetsUpdatesAndSoftDeletesDeworming() throws Exception {
        String token = registerAndLogin("Deworm_Crud_" + suffix());
        long petId = createPet(token, "DewormPet");
        LocalDate today = today();
        String path = path(petId);

        MvcResult created = mockMvc.perform(post(path)
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(payload("EXTERNAL", "大宠爱", today.minusDays(8), today.plusDays(22))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type").value("EXTERNAL"))
                .andExpect(jsonPath("$.data.medicineName").value("大宠爱"))
                .andExpect(jsonPath("$.data.dewormingDate").value(today.minusDays(8).toString()))
                .andReturn();
        long recordId = longField(created, "id");

        mockMvc.perform(get(path).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
        mockMvc.perform(get(path + "/" + recordId).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("EXTERNAL"));

        mockMvc.perform(put(path + "/" + recordId)
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(payload("BOTH", "海乐妙", today.minusDays(8), today.plusDays(30))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("BOTH"))
                .andExpect(jsonPath("$.data.medicineName").value("海乐妙"));

        mockMvc.perform(delete(path + "/" + recordId).header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        mockMvc.perform(get(path + "/" + recordId).header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(path).header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void validatesEnumDatesAndAllowsMultipleRecordsOnOneDay() throws Exception {
        String token = registerAndLogin("Deworm_Date_" + suffix());
        long petId = createPet(token, "DewormDatePet");
        String path = path(petId);
        LocalDate today = today();

        mockMvc.perform(post(path).header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(payload("INTERNAL", null, today, today.plusDays(30))))
                .andExpect(status().isCreated());
        mockMvc.perform(post(path).header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(payload("EXTERNAL", null, today, today.plusDays(30))))
                .andExpect(status().isCreated());
        mockMvc.perform(get(path).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(post(path).header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(payload("INVALID", null, today, today.plusDays(30))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(path).header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(payload("BOTH", null, today, today.minusDays(1))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(path).header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(payload("INTERNAL", null, today.plusDays(366), null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void hidesOtherUsersRecordsAndRejectsAnonymousAccess() throws Exception {
        String ownerToken = registerAndLogin("Deworm_Owner_" + suffix());
        String otherToken = registerAndLogin("Deworm_Other_" + suffix());
        long ownerPetId = createPet(ownerToken, "OwnerPet");
        long otherPetId = createPet(otherToken, "OtherPet");
        LocalDate today = today();
        MvcResult created = mockMvc.perform(post(path(ownerPetId))
                        .header("Authorization", bearer(ownerToken))
                        .contentType(APPLICATION_JSON)
                        .content(payload("INTERNAL", "驱虫药", today, today.plusDays(25))))
                .andExpect(status().isCreated())
                .andReturn();
        long recordId = longField(created, "id");

        mockMvc.perform(get(path(ownerPetId)).header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(path(ownerPetId) + "/" + recordId).header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(path(otherPetId) + "/" + recordId).header("Authorization", bearer(ownerToken)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(path(ownerPetId))).andExpect(status().isUnauthorized());
    }

    private String registerAndLogin(String username) throws Exception {
        String password = TestCredentials.newPassword();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isCreated());
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return stringField(login, "accessToken");
    }

    private long createPet(String token, String name) throws Exception {
        MvcResult created = mockMvc.perform(post("/api/pets")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"type\":\"CAT\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return longField(created, "id");
    }

    private String payload(String type, String medicine, LocalDate date, LocalDate nextDate) {
        String medicineValue = medicine == null ? "null" : "\"" + medicine + "\"";
        String nextDateValue = nextDate == null ? "null" : "\"" + nextDate + "\"";
        return """
                {"type":"%s","medicineName":%s,"dewormingDate":"%s","nextDate":%s}
                """.formatted(type, medicineValue, date, nextDateValue);
    }

    private long longField(MvcResult result, String field) throws Exception {
        String json = result.getResponse().getContentAsString();
        String marker = "\"" + field + "\":";
        int start = json.indexOf(marker);
        if (start < 0) {
            throw new AssertionError("JSON field not found: " + field);
        }
        start += marker.length();
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) {
            end++;
        }
        return Long.parseLong(json.substring(start, end));
    }

    private String stringField(MvcResult result, String field) throws Exception {
        String json = result.getResponse().getContentAsString();
        String marker = "\"" + field + "\":\"";
        int start = json.indexOf(marker);
        if (start < 0) {
            throw new AssertionError("JSON field not found: " + field);
        }
        start += marker.length();
        return json.substring(start, json.indexOf('"', start));
    }

    private LocalDate today() {
        return LocalDate.now(ZoneId.of("Asia/Shanghai"));
    }

    private String path(long petId) {
        return "/api/pets/" + petId + "/dewormings";
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
