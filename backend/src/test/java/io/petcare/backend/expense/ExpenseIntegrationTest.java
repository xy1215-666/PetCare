package io.petcare.backend.expense;

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
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
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
class ExpenseIntegrationTest {

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
    void createsFiltersUpdatesAndSoftDeletesExpenseRecords() throws Exception {
        String token = registerAndLogin("Expense_Crud_" + suffix());
        long petA = createPet(token, "ExpensePetA");
        long petB = createPet(token, "ExpensePetB");
        LocalDate today = today();
        long foodId = createExpense(token, petA, "FOOD", "129.90", today, "猫粮");
        createExpense(token, petB, "MEDICAL", "300.00", today.minusDays(2), "检查");
        createExpense(token, null, "DAILY_SUPPLIES", "50.00", today.minusDays(10), "猫砂");

        mockMvc.perform(get("/api/expenses").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.records.length()").value(3))
                .andExpect(jsonPath("$.data.records[0].amount").value(129.90))
                .andExpect(jsonPath("$.data.records[0].petName").value("ExpensePetA"));
        mockMvc.perform(get("/api/expenses").header("Authorization", bearer(token))
                        .param("petId", String.valueOf(petA)).param("category", "FOOD")
                        .param("startDate", today.toString()).param("endDate", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(foodId));
        mockMvc.perform(get("/api/expenses").header("Authorization", bearer(token))
                        .param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(3));
        mockMvc.perform(get("/api/expenses").header("Authorization", bearer(token))
                        .param("startDate", today.toString()).param("endDate", today.minusDays(1).toString()))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/expenses/" + foodId).header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(foodId));
        mockMvc.perform(put("/api/expenses/" + foodId).header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(expenseJson(petA, "FOOD", "139.50", today, "新猫粮")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value(139.50))
                .andExpect(jsonPath("$.data.description").value("新猫粮"));
        mockMvc.perform(delete("/api/expenses/" + foodId).header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/expenses/" + foodId).header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT deleted FROM expense_record WHERE id = ?", Integer.class, foodId)).isEqualTo(1);
    }

    @Test
    void isolatesExpensesAndRejectsForeignPetBinding() throws Exception {
        String usernameA = "Expense_A_" + suffix();
        String tokenA = registerAndLogin(usernameA);
        String tokenB = registerAndLogin("Expense_B_" + suffix());
        long petA = createPet(tokenA, "OwnerAPet");
        MvcResult createdA = mockMvc.perform(post("/api/expenses").header("Authorization", bearer(tokenA))
                        .contentType(APPLICATION_JSON)
                        .content(expenseJson(petA, "FOOD", "40.00", today(), "记录A")
                                .replace("{", "{\"userId\":999999,")))
                .andExpect(status().isCreated()).andReturn();
        long expenseA = extractLong(createdA.getResponse().getContentAsString(), "\"id\":");
        Long expectedOwnerId = jdbcTemplate.queryForObject(
                "SELECT id FROM app_user WHERE username = ?", Long.class, usernameA.toLowerCase());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT user_id FROM expense_record WHERE id = ?", Long.class, expenseA))
                .isEqualTo(expectedOwnerId).isNotEqualTo(999999L);
        long petB = createPet(tokenB, "OwnerBPet");
        long expenseB = createExpense(tokenB, petB, "FOOD", "20.00", today(), "记录B");

        mockMvc.perform(get("/api/expenses").header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(expenseB));
        mockMvc.perform(get("/api/expenses/" + expenseA).header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/expenses/" + expenseA).header("Authorization", bearer(tokenB))
                        .contentType(APPLICATION_JSON)
                        .content(expenseJson(petB, "FOOD", "15.00", today(), "越权修改")))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/expenses/" + expenseA).header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/expenses").header("Authorization", bearer(tokenA))
                        .contentType(APPLICATION_JSON)
                        .content(expenseJson(petB, "FOOD", "15.00", today(), "跨用户宠物")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/expenses"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));
        mockMvc.perform(post("/api/expenses").contentType(APPLICATION_JSON)
                        .content(expenseJson(null, "FOOD", "1.00", today(), "匿名")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidAmountsCategoriesDatesAndPets() throws Exception {
        String token = registerAndLogin("Expense_Validation_" + suffix());
        long petId = createPet(token, "ValidationPet");
        LocalDate today = today();
        for (String amount : new String[]{"0", "-1", "1000000.00", "1.001"}) {
            mockMvc.perform(post("/api/expenses").header("Authorization", bearer(token))
                            .contentType(APPLICATION_JSON)
                            .content(expenseJson(petId, "FOOD", amount, today, "错误金额")))
                    .andExpect(status().isBadRequest());
        }
        mockMvc.perform(post("/api/expenses").header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(expenseJson(petId, "INVALID", "1.00", today, "非法分类")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/expenses").header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(expenseJson(petId, "FOOD", "1.00", today.plusDays(1), "未来日期")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/expenses").header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(expenseJson(9_223_372_036_854_775_000L, "FOOD", "1.00", today, "不存在宠物")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/expenses").header("Authorization", bearer(token)).param("category", "INVALID"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/expenses").header("Authorization", bearer(token)).param("startDate", "2026-02-30"))
                .andExpect(status().isBadRequest());
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM expense_record", Integer.class)).isZero();
    }

    @Test
    void calculatesMonthlyCategoryTrendAndPetStatisticsWithZeroFilledMonths() throws Exception {
        String token = registerAndLogin("Expense_Stats_" + suffix());
        long petA = createPet(token, "StatsPetA");
        long petB = createPet(token, "StatsPetB");
        YearMonth current = YearMonth.from(today());
        YearMonth previous = current.minusMonths(1);

        createExpense(token, petA, "FOOD", "100.00", previous.atDay(3), "上月食品");
        createExpense(token, petA, "MEDICAL", "50.00", previous.atDay(8), "上月医疗");
        createExpense(token, petA, "FOOD", "50.00", today(), "本月食品");
        createExpense(token, null, "DAILY_SUPPLIES", "25.00", today(), "公共用品");
        createExpense(token, petA, "TOY", "18.00", current.minusMonths(3).atDay(1), "趋势数据");

        mockMvc.perform(get("/api/expenses/summary/monthly").header("Authorization", bearer(token))
                        .param("month", current.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.month").value(current.toString()))
                .andExpect(jsonPath("$.data.totalAmount").value(75.00))
                .andExpect(jsonPath("$.data.recordCount").value(2))
                .andExpect(jsonPath("$.data.previousMonthAmount").value(150.00))
                .andExpect(jsonPath("$.data.changeRate").value(-50.00));
        mockMvc.perform(get("/api/expenses/summary/monthly").header("Authorization", bearer(token))
                        .param("month", current.toString()).param("petId", String.valueOf(petB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(0.00))
                .andExpect(jsonPath("$.data.changeRate").value(nullValue()));

        mockMvc.perform(get("/api/expenses/statistics/categories").header("Authorization", bearer(token))
                        .param("startDate", current.atDay(1).toString())
                        .param("endDate", current.atEndOfMonth().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].category").value("FOOD"))
                .andExpect(jsonPath("$.data[0].amount").value(50.00));
        mockMvc.perform(get("/api/expenses/statistics/categories").header("Authorization", bearer(token))
                        .param("startDate", current.atDay(1).toString())
                        .param("endDate", current.atEndOfMonth().toString())
                        .param("petId", String.valueOf(petA)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].amount").value(50.00));

        MvcResult trend = mockMvc.perform(get("/api/expenses/statistics/monthly-trend")
                        .header("Authorization", bearer(token)).param("months", "6"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(6))
                .andExpect(jsonPath("$.data[0].month").value(current.minusMonths(5).toString()))
                .andExpect(jsonPath("$.data[2].amount").value(18.00))
                .andExpect(jsonPath("$.data[3].amount").value(0.00))
                .andExpect(jsonPath("$.data[5].month").value(current.toString()))
                .andReturn();
        assertThat(trend.getResponse().getContentAsString()).contains("\"month\":\"" + current.minusMonths(5) + "\"");

        mockMvc.perform(get("/api/expenses/statistics/by-pet").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].petId").value(petA))
                .andExpect(jsonPath("$.data[0].amount").value(218.00))
                .andExpect(jsonPath("$.data[1].petName").value("未指定宠物"))
                .andExpect(jsonPath("$.data[1].amount").value(25.00))
                .andExpect(jsonPath("$.data[2].petId").value(petB))
                .andExpect(jsonPath("$.data[2].amount").value(0.00));

        String otherToken = registerAndLogin("Exp_Other_" + suffix());
        mockMvc.perform(get("/api/expenses/summary/monthly").header("Authorization", bearer(otherToken))
                        .param("month", current.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalAmount").value(0.00));
    }

    private long createPet(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/pets")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"type\":\"CAT\"}"))
                .andExpect(status().isCreated()).andReturn();
        return extractLong(result.getResponse().getContentAsString(), "\"id\":");
    }

    private long createExpense(String token, Long petId, String category, String amount,
                               LocalDate date, String description) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/expenses")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(expenseJson(petId, category, amount, date, description)))
                .andExpect(status().isCreated()).andReturn();
        return extractLong(result.getResponse().getContentAsString(), "\"id\":");
    }

    private String registerAndLogin(String username) throws Exception {
        String password = TestCredentials.newPassword();
        mockMvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isCreated());
        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andReturn();
        String response = result.getResponse().getContentAsString();
        String marker = "\"accessToken\":\"";
        int start = response.indexOf(marker);
        assertThat(start).isGreaterThanOrEqualTo(0);
        start += marker.length();
        return response.substring(start, response.indexOf('"', start));
    }

    private String expenseJson(Long petId, String category, String amount, LocalDate date, String description) {
        String pet = petId == null ? "null" : String.valueOf(petId);
        return """
                {"petId":%s,"category":"%s","amount":%s,"expenseDate":"%s","description":"%s"}
                """.formatted(pet, category, amount, date, description);
    }

    private String bearer(String token) { return "Bearer " + token; }
    private LocalDate today() { return LocalDate.now(ZoneId.of("Asia/Shanghai")); }

    private long extractLong(String json, String marker) {
        int start = json.indexOf(marker);
        assertThat(start).isGreaterThanOrEqualTo(0);
        start += marker.length();
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
        return Long.parseLong(json.substring(start, end));
    }

    private String suffix() { return UUID.randomUUID().toString().replace("-", "").substring(0, 10); }
}
