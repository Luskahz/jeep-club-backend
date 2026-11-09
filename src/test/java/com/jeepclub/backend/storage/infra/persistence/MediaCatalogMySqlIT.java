package com.jeepclub.backend.storage.infra.persistence;

import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MediaCatalogMySqlIT extends MediaCatalogPersistenceTest {
    @Test void flywayRejectsLowercaseVariantCodes() {
        var asset = asset();
        var variant = catalog.registerVariant(variant(asset.mediaId(), "AVATAR", "images/uppercase-check"), audit());
        assertThatThrownBy(() -> jdbc.update("update storage_media_variants set variant_code = ? where id = ?", "avatar", variant.variantId()))
                .isInstanceOf(RuntimeException.class);
    }
    @Container static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.46");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", MYSQL::getJdbcUrl); r.add("spring.datasource.username", MYSQL::getUsername);
        r.add("spring.datasource.password", MYSQL::getPassword); r.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        r.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "validate"); r.add("spring.flyway.enabled", () -> true);
    }
}
