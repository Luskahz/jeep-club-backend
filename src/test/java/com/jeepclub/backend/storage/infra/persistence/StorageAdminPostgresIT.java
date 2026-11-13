package com.jeepclub.backend.storage.infra.persistence;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** PostgreSQL uses Hibernate DDL; the project's Flyway chain targets MySQL. */
@Testcontainers
class StorageAdminPostgresIT extends StorageUploadFlowTest {
    @Container static final PostgreSQLContainer POSTGRES=new PostgreSQLContainer("postgres:17.6-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",POSTGRES::getJdbcUrl);r.add("spring.datasource.username",POSTGRES::getUsername);
        r.add("spring.datasource.password",POSTGRES::getPassword);r.add("spring.datasource.driver-class-name",() -> "org.postgresql.Driver");
        r.add("spring.jpa.database-platform",() -> "org.hibernate.dialect.PostgreSQLDialect");
        r.add("spring.jpa.properties.hibernate.hbm2ddl.halt_on_error",() -> true);
    }
}
