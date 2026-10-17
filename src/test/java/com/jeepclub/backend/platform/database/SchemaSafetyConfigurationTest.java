package com.jeepclub.backend.platform.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.*;

class SchemaSafetyConfigurationTest {
    @Test void acceptsSafeDefaults() {
        assertThatCode(() -> SchemaSafetyConfiguration.validate(new MockEnvironment())).doesNotThrowAnyException();
    }
    @ParameterizedTest
    @CsvSource({
            "spring.jpa.hibernate.ddl-auto,update", "spring.jpa.hibernate.ddl-auto,create-drop",
            "spring.flyway.enabled,false", "spring.flyway.baseline-on-migrate,true",
            "spring.flyway.clean-disabled,false", "spring.flyway.validate-on-migrate,false",
            "spring.jpa.generate-ddl,true", "spring.sql.init.mode,always",
            "spring.jpa.properties.hibernate.hbm2ddl.auto,update",
            "spring.jpa.properties.jakarta.persistence.schema-generation.database.action,create",
            "spring.jpa.defer-datasource-initialization,true",
            "spring.flyway.out-of-order,true", "spring.flyway.ignore-migration-patterns,*:future",
            "spring.flyway.validate-migration-naming,false", "spring.flyway.locations,filesystem:/tmp/sql",
            "spring.flyway.url,jdbc:mysql://other/database", "spring.flyway.target,2"
    })
    void rejectsUnsafeOverrides(String key, String value) {
        assertThatThrownBy(() -> SchemaSafetyConfiguration.validate(new MockEnvironment().withProperty(key, value)))
                .isInstanceOf(IllegalStateException.class).hasMessageStartingWith("Unsafe database configuration:");
    }
    @Test void permitsLegacyH2SlicesOnlyInMemoryAndWithTestProfile() {
        var env = new MockEnvironment().withProperty("spring.datasource.url", "jdbc:h2:mem:focused")
                .withProperty("spring.flyway.enabled", "false").withProperty("spring.jpa.hibernate.ddl-auto", "create-drop");
        env.setActiveProfiles("test");
        assertThatCode(() -> SchemaSafetyConfiguration.validate(env)).doesNotThrowAnyException();
        env.setProperty("spring.datasource.url", "jdbc:mysql://localhost/test");
        assertThatThrownBy(() -> SchemaSafetyConfiguration.validate(env)).isInstanceOf(IllegalStateException.class);
    }
}
