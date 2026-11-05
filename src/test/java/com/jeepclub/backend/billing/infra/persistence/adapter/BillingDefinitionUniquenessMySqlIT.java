package com.jeepclub.backend.billing.infra.persistence.adapter;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class BillingDefinitionUniquenessMySqlIT extends BillingDefinitionUniquenessIntegrationTest {
    @Container static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.46")
            .withUsername("root").withPassword("synthetic-billing-test-only");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> true);
    }

    @org.junit.jupiter.api.Test void migrationProtectsCaseSensitiveNameCollation() throws Exception {
        try (var connection = java.sql.DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("CREATE TEMPORARY TABLE billing_charge_definitions "
                    + "(name varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_cs NOT NULL, UNIQUE(name))");
            org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(connection,
                    new org.springframework.core.io.ClassPathResource("db/migration/V14__billing_definition_case_insensitive_name.sql"));
            statement.executeUpdate("INSERT INTO billing_charge_definitions(name) VALUES ('MeNsAl')");
            org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                    statement.executeUpdate("INSERT INTO billing_charge_definitions(name) VALUES ('mensal')"))
                    .isInstanceOfSatisfying(java.sql.SQLException.class,
                            error -> org.assertj.core.api.Assertions.assertThat(error.getErrorCode()).isEqualTo(1062));
        }
    }
}
