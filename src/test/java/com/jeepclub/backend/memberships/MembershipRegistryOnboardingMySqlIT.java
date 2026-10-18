package com.jeepclub.backend.memberships;

import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

/** Repeats the real HTTP, Identity, Authentication and rollback scenarios against Flyway + MySQL validate. */
@Testcontainers
class MembershipRegistryOnboardingMySqlIT extends MembershipActivationFlowIntegrationTest {
    @Container static final MySQLContainer MYSQL=new MySQLContainer("mysql:8.0.46").withUsername("root").withPassword("synthetic-test-only");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",MYSQL::getJdbcUrl);registry.add("spring.datasource.username",MYSQL::getUsername);
        registry.add("spring.datasource.password",MYSQL::getPassword);registry.add("spring.datasource.driver-class-name",() -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.database-platform",() -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto",() -> "validate");registry.add("spring.flyway.enabled",() -> true);
    }
}
