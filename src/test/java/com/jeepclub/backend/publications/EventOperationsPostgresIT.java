package com.jeepclub.backend.publications;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Real PostgreSQL repetition of Event/Billing races, nullable uniques and historical deletion. */
@Testcontainers
@org.springframework.boot.test.context.SpringBootTest(classes=EventOperationsPostgresIT.PostgresApplication.class)
@org.springframework.test.annotation.DirtiesContext(classMode=org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class EventOperationsPostgresIT extends EventOperationsIntegrationTest {
    /** Container-only context. Production schema guard is excluded, never relaxed in application code. */
    @org.springframework.boot.autoconfigure.EnableAutoConfiguration
    @org.springframework.boot.autoconfigure.AutoConfigurationPackage(basePackages="com.jeepclub.backend")
    @org.springframework.context.annotation.ComponentScan(basePackages="com.jeepclub.backend",excludeFilters={
        @org.springframework.context.annotation.ComponentScan.Filter(type=org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,classes={com.jeepclub.backend.BackendApplication.class,com.jeepclub.backend.platform.database.SchemaSafetyConfiguration.class}),
        @org.springframework.context.annotation.ComponentScan.Filter(type=org.springframework.context.annotation.FilterType.CUSTOM,classes={org.springframework.boot.context.TypeExcludeFilter.class,org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter.class})})
    static class PostgresApplication {}
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.hbm2ddl.halt_on_error", () -> "true");
    }
    @Override long currentDatabaseSession() { return jdbc.queryForObject("select pg_backend_pid()",Long.class); }
    @Override boolean isDatabaseSessionWaitingForLock(long session) {
        return Boolean.TRUE.equals(jdbc.queryForObject("select cardinality(pg_blocking_pids(?::integer)) > 0",Boolean.class,session));
    }
}
