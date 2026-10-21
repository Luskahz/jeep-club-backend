package com.jeepclub.backend.vehicles.api.controller;

import org.springframework.test.context.*;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
class VehicleGalleryMySqlIT extends VehicleGalleryIntegrationTest {
    @Container static final MySQLContainer MYSQL=new MySQLContainer("mysql:8.0.46").withUsername("root").withPassword("synthetic-vehicle-gallery-test-only");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",MYSQL::getJdbcUrl);registry.add("spring.datasource.username",MYSQL::getUsername);
        registry.add("spring.datasource.password",MYSQL::getPassword);registry.add("spring.datasource.driver-class-name",() -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.database-platform",() -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto",() -> "validate");registry.add("spring.flyway.enabled",() -> true);
    }
}
