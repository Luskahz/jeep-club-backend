package com.jeepclub.backend.billing.infra.persistence.adapter;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class BillingDefinitionUniquenessPostgresIT extends BillingDefinitionUniquenessIntegrationTest {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.properties.hibernate.hbm2ddl.halt_on_error", () -> "true");
    }
    @org.junit.jupiter.api.Test void migrationProtectsCaseSensitiveNames() throws Exception {
        try (var connection = java.sql.DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("CREATE TEMPORARY TABLE billing_charge_definitions (name varchar(120) NOT NULL UNIQUE)");
            org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(connection,
                new org.springframework.core.io.ClassPathResource("db/migration/V14__billing_definition_case_insensitive_name.sql"));
            statement.executeUpdate("INSERT INTO billing_charge_definitions(name) VALUES ('MeNsAl')");
            org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                statement.executeUpdate("INSERT INTO billing_charge_definitions(name) VALUES ('mensal')"))
                .isInstanceOfSatisfying(java.sql.SQLException.class,
                    error -> org.assertj.core.api.Assertions.assertThat(error.getSQLState()).isEqualTo("23505"));
        }
    }
}
