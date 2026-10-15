package com.jeepclub.backend.platform.database;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/** Reject dangerous overrides before Flyway or an EntityManagerFactory can be created. */
@Configuration(proxyBeanMethods = false)
public class SchemaSafetyConfiguration {
    @Bean
    static BeanFactoryPostProcessor schemaSafetyCheck(Environment environment) {
        return beanFactory -> validate(environment);
    }

    static void validate(Environment env) {
        // Existing focused H2 tests remain fast; mandatory MySQL CI validates the migrations.
        if (env.acceptsProfiles(Profiles.of("test"))
                && env.getProperty("spring.datasource.url", "").startsWith("jdbc:h2:mem:")) {
            return;
        }
        require("validate".equals(env.getProperty("spring.jpa.hibernate.ddl-auto", "validate")),
                "Hibernate ddl-auto must be validate");
        require(env.getProperty("spring.flyway.enabled", Boolean.class, true), "Flyway must be enabled");
        require(!env.getProperty("spring.flyway.baseline-on-migrate", Boolean.class, false),
                "Automatic baseline is forbidden; verify equivalence and baseline explicitly");
        require(env.getProperty("spring.flyway.clean-disabled", Boolean.class, true), "Flyway clean must be disabled");
        require(env.getProperty("spring.flyway.validate-on-migrate", Boolean.class, true), "Flyway validation must be enabled");
        require(!env.getProperty("spring.jpa.generate-ddl", Boolean.class, false), "JPA DDL generation is forbidden");
        require("never".equals(env.getProperty("spring.sql.init.mode", "never")), "SQL initializer must be disabled");
        require("validate".equals(env.getProperty("spring.jpa.properties.hibernate.hbm2ddl.auto", "validate")),
                "Direct Hibernate DDL override is forbidden");
        require("none".equals(env.getProperty("spring.jpa.properties.jakarta.persistence.schema-generation.database.action", "none")),
                "Jakarta schema generation is forbidden");
        require(!env.getProperty("spring.jpa.defer-datasource-initialization", Boolean.class, false),
                "Flyway must initialize before Hibernate");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Unsafe database configuration: " + message);
    }
}
