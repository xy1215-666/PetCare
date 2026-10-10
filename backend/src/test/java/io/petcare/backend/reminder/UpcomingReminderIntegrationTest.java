package io.petcare.backend.reminder;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class UpcomingReminderIntegrationTest {

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
    void aggregatesVaccinationDewormingAndMedicalRemindersInDateOrder() throws Exception {
        String token = registerAndLogin("Reminder_All_" + suffix());
        long petId = createPet(token, "小白");
        LocalDate today = today();

        createVaccination(token, petId, "狂犬疫苗", today.minusDays(20), today.plusDays(12));
        createDeworming(token, petId, "EXTERNAL", today.minusDays(8), today.plusDays(5));
        createMedical(token, petId, today.minusDays(2), today.plusDays(18));

        mockMvc.perform(get("/api/reminders/upcoming").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].type").value("DEWORMING"))
                .andExpect(jsonPath("$.data[0].title").value("体外驱虫"))
                .andExpect(jsonPath("$.data[0].petName").value("小白"))
                .andExpect(jsonPath("$.data[0].daysRemaining").value(5))
                .andExpect(jsonPath("$.data[1].type").value("VACCINATION"))
                .andExpect(jsonPath("$.data[1].title").value("狂犬疫苗"))
                .andExpect(jsonPath("$.data[1].daysRemaining").value(12))
                .andExpect(jsonPath("$.data[2].type").value("MEDICAL_REVISIT"))
                .andExpect(jsonPath("$.data[2].title").value("安心宠物医院复诊"))
                .andExpect(jsonPath("$.data[2].daysRemaining").value(18));
    }

    @Test
    void includesTodayAndDayThirtyButExcludesPastAndDayThirtyOne() throws Exception {
        String token = registerAndLogin("Reminder_Window_" + suffix());
        long petId = createPet(token, "WindowPet");
        LocalDate today = today();
        LocalDate recordDate = today.minusDays(5);

        createVaccination(token, petId, "今天到期", recordDate, today);
        createVaccination(token, petId, "三十天到期", recordDate, today.plusDays(30));
        createVaccination(token, petId, "超出窗口", recordDate, today.plusDays(31));
        createVaccination(token, petId, "已经过期", recordDate, today.minusDays(1));

        mockMvc.perform(get("/api/reminders/upcoming").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].title").value("今天到期"))
                .andExpect(jsonPath("$.data[0].daysRemaining").value(0))
                .andExpect(jsonPath("$.data[1].title").value("三十天到期"))
                .andExpect(jsonPath("$.data[1].daysRemaining").value(30));
    }

    @Test
    void returnsOnlyCurrentUsersRemindersAndRequiresAuthentication() throws Exception {
        String ownerToken = registerAndLogin("Reminder_Owner_" + suffix());
        String otherToken = registerAndLogin("Reminder_Other_" + suffix());
        long petId = createPet(ownerToken, "OwnerPet");
        LocalDate today = today();
        createVaccination(ownerToken, petId, "狂犬疫苗", today.minusDays(1), today.plusDays(4));

        mockMvc.perform(get("/api/reminders/upcoming").header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
        mockMvc.perform(get("/api/reminders/upcoming").header("Authorization", bearer(otherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
        mockMvc.perform(get("/api/reminders/upcoming")).andExpect(status().isUnauthorized());
    }

    private void createVaccination(String token, long petId, String name, LocalDate date, LocalDate nextDate)
            throws Exception {
        mockMvc.perform(post("/api/pets/" + petId + "/vaccinations")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"vaccineName":"%s","vaccinationDate":"%s","nextDate":"%s"}
                                """.formatted(name, date, nextDate)))
                .andExpect(status().isCreated());
    }

    private void createDeworming(String token, long petId, String type, LocalDate date, LocalDate nextDate)
            throws Exception {
        mockMvc.perform(post("/api/pets/" + petId + "/dewormings")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"type":"%s","dewormingDate":"%s","nextDate":"%s"}
                                """.formatted(type, date, nextDate)))
                .andExpect(status().isCreated());
    }

    private void createMedical(String token, long petId, LocalDate visitDate, LocalDate nextVisitDate)
            throws Exception {
        mockMvc.perform(post("/api/pets/" + petId + "/medical-records")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"visitDate":"%s","hospital":"安心宠物医院","reason":"复查","cost":50.00,"nextVisitDate":"%s"}
                                """.formatted(visitDate, nextVisitDate)))
                .andExpect(status().isCreated());
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
        String json = login.getResponse().getContentAsString();
        String marker = "\"accessToken\":\"";
        int start = json.indexOf(marker) + marker.length();
        return json.substring(start, json.indexOf('"', start));
    }

    private long createPet(String token, String name) throws Exception {
        MvcResult created = mockMvc.perform(post("/api/pets")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"type\":\"CAT\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String json = created.getResponse().getContentAsString();
        String marker = "\"id\":";
        int start = json.indexOf(marker) + marker.length();
        int end = start;
        while (Character.isDigit(json.charAt(end))) {
            end++;
        }
        return Long.parseLong(json.substring(start, end));
    }

    private LocalDate today() {
        return LocalDate.now(ZoneId.of("Asia/Shanghai"));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
