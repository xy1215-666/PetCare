package io.petcare.backend.medical;

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

import java.math.BigDecimal;
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
class MedicalRecordIntegrationTest {

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
    void createsListsGetsUpdatesAndSoftDeletesMedicalRecord() throws Exception {
        String token = registerAndLogin("Medical_Crud_" + suffix());
        long petId = createPet(token, "MedicalPet");
        LocalDate today = today();
        String path = path(petId);

        MvcResult created = mockMvc.perform(post(path)
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(payload(today.minusDays(5), "食欲下降", "320.75", today.plusDays(2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.reason").value("食欲下降"))
                .andExpect(jsonPath("$.data.cost").value(320.75))
                .andExpect(jsonPath("$.data.result").value("用户记录的检查结果"))
                .andReturn();
        long recordId = longField(created, "id");

        mockMvc.perform(get(path).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
        mockMvc.perform(get(path + "/" + recordId).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hospital").value("安心宠物医院"));

        mockMvc.perform(put(path + "/" + recordId)
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(payload(today.minusDays(5), "复查", "0.00", today.plusDays(7))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cost").value(0.00))
                .andExpect(jsonPath("$.data.reason").value("复查"));

        mockMvc.perform(delete(path + "/" + recordId).header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        mockMvc.perform(get(path + "/" + recordId).header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(path).header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void rejectsNegativeCostInvalidScaleAndInvalidDates() throws Exception {
        String token = registerAndLogin("Medical_Valid_" + suffix());
        long petId = createPet(token, "MedicalValidationPet");
        String path = path(petId);
        LocalDate today = today();

        mockMvc.perform(post(path).header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(payload(today, "检查", "-0.01", null)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(path).header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(payload(today, "检查", "1.001", null)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(path).header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(payload(today.plusDays(1), "未来就诊", "0.00", null)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(path).header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(payload(today, "日期倒置", "0.00", today.minusDays(1))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(path).header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(payload(today, "   ", "0.00", null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void hidesOtherUsersRecordsAndRejectsAnonymousAccess() throws Exception {
        String ownerToken = registerAndLogin("Medical_Owner_" + suffix());
        String otherToken = registerAndLogin("Medical_Other_" + suffix());
        long ownerPetId = createPet(ownerToken, "OwnerPet");
        long otherPetId = createPet(otherToken, "OtherPet");
        LocalDate today = today();
        MvcResult created = mockMvc.perform(post(path(ownerPetId))
                        .header("Authorization", bearer(ownerToken))
                        .contentType(APPLICATION_JSON)
                        .content(payload(today, "体检", "100.00", null)))
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

    private String payload(LocalDate visitDate, String reason, String cost, LocalDate nextVisitDate) {
        String nextDateValue = nextVisitDate == null ? "null" : "\"" + nextVisitDate + "\"";
        return """
                {"visitDate":"%s","hospital":"安心宠物医院","reason":"%s","examination":"基础检查","result":"用户记录的检查结果","medicine":"药品","doctorAdvice":"按医嘱观察","cost":%s,"nextVisitDate":%s}
                """.formatted(visitDate, reason, cost, nextDateValue);
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
        return "/api/pets/" + petId + "/medical-records";
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
