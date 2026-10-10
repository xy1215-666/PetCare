package io.petcare.backend;

import io.petcare.backend.bootstrap.BootstrapProbeMapper;
import io.petcare.backend.support.TestCredentials;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class MySql84BootstrapTest {

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
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BootstrapProbeMapper probeMapper;

    @Test
    void mysql84FlywayJdbcAndMyBatisPlusWorkTogether() {
        String version = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
        String migrationVersion = jdbcTemplate.queryForObject(
                "SELECT version FROM flyway_schema_history WHERE success = 1 ORDER BY installed_rank DESC LIMIT 1",
                String.class);

        assertThat(version).startsWith("8.4.");
        assertThat(migrationVersion).isEqualTo("5");
        assertThat(probeMapper.selectOne()).isEqualTo(1);
    }

    @Test
    void userTableHasUniqueKeysForUsernameAndOptionalContactFields() {
        var uniqueIndexes = jdbcTemplate.queryForList(
                "SELECT INDEX_NAME FROM information_schema.statistics " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_user' AND NON_UNIQUE = 0",
                String.class);

        assertThat(uniqueIndexes)
                .contains("uk_app_user_username", "uk_app_user_email", "uk_app_user_phone");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT IS_NULLABLE FROM information_schema.columns " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_user' AND COLUMN_NAME = 'email'",
                String.class)).isEqualTo("YES");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT IS_NULLABLE FROM information_schema.columns " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_user' AND COLUMN_NAME = 'phone'",
                String.class)).isEqualTo("YES");
    }

    @Test
    void petTableHasUserOwnershipIndexAndConstrainedEnums() {
        var userIndexColumns = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME FROM information_schema.statistics " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pet' " +
                        "AND INDEX_NAME = 'idx_pet_user_deleted' ORDER BY SEQ_IN_INDEX",
                String.class);

        assertThat(userIndexColumns).containsExactly("user_id", "deleted");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pet' " +
                        "AND COLUMN_NAME IN ('type', 'gender', 'current_weight', 'home_date')",
                Integer.class)).isEqualTo(4);
    }

    @Test
    void expenseTableUsesNullablePetAndPreciseMoneyWithUserScopedIndexes() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT IS_NULLABLE FROM information_schema.columns " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'expense_record' AND COLUMN_NAME = 'pet_id'",
                String.class)).isEqualTo("YES");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT CONCAT(DATA_TYPE, ':', NUMERIC_PRECISION, ':', NUMERIC_SCALE) " +
                        "FROM information_schema.columns WHERE TABLE_SCHEMA = DATABASE() " +
                        "AND TABLE_NAME = 'expense_record' AND COLUMN_NAME = 'amount'",
                String.class)).isEqualTo("decimal:10:2");
        var dateIndexColumns = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME FROM information_schema.statistics " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'expense_record' " +
                        "AND INDEX_NAME = 'idx_expense_user_date' ORDER BY SEQ_IN_INDEX",
                String.class);
        assertThat(dateIndexColumns).containsExactly("user_id", "deleted", "expense_date", "id");
    }
}
