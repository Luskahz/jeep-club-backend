package com.jeepclub.backend.platform.web.cors;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.support.ResourcePropertySource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(context -> {
                try {
                    context.getEnvironment().getPropertySources()
                            .addLast(new ResourcePropertySource("classpath:application.properties"));
                } catch (IOException exception) {
                    throw new IllegalStateException(exception);
                }
            })
            .withUserConfiguration(CorsConfig.class);

    @Test
    void defaultsPreserveExistingCorsRules() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            CorsProperties properties = context.getBean(CorsProperties.class);
            assertThat(properties.getAllowedOrigins()).containsExactly(
                    "http://localhost:8080",
                    "http://localhost:3000",
                    "https://jeep-club-backend-production.up.railway.app"
            );
            assertThat(properties.getAllowedMethods()).containsExactly("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
            assertThat(properties.getAllowedHeaders()).containsExactly("*");
            assertThat(properties.getAllowCredentials()).isTrue();
            assertThat(properties.getMaxAge()).isEqualTo(3600L);
        });
    }

    @Test
    void bindsCommaSeparatedOriginsAndOtherOverrides() {
        contextRunner.withPropertyValues(
                "app.cors.allowed-origins=https://frontend.example.com,https://admin.example.com",
                "app.cors.allowed-methods=GET,POST",
                "app.cors.allowed-headers=Content-Type,Authorization",
                "app.cors.allow-credentials=false",
                "app.cors.max-age=120"
        ).run(context -> {
            assertThat(context).hasNotFailed();
            CorsProperties properties = context.getBean(CorsProperties.class);
            assertThat(properties.getAllowedOrigins()).containsExactly("https://frontend.example.com", "https://admin.example.com");
            assertThat(properties.getAllowedMethods()).containsExactly("GET", "POST");
            assertThat(properties.getAllowedHeaders()).containsExactly("Content-Type", "Authorization");
            assertThat(properties.getAllowCredentials()).isFalse();
            assertThat(properties.getMaxAge()).isEqualTo(120L);
        });
    }

    @Test
    void rejectsNegativeMaxAgeAtStartup() {
        contextRunner.withPropertyValues("app.cors.max-age=-1").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasMessageContaining("app.cors");
        });
    }
}
