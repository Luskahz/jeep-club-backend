package com.jeepclub.backend.platform.web.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class ApiProblemFactoryTest {

    private static final Instant NOW = Instant.parse("2026-08-05T12:00:00Z");

    private ApiProblemFactory problemFactory;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);

        problemFactory = new ApiProblemFactory(
                messageSource,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void createsProblemInPortuguese() {
        ApiErrorResponse problem = problemFactory.create(
                HttpStatus.NOT_FOUND,
                "MEMBERSHIP_APPLICATION_NOT_FOUND",
                Locale.forLanguageTag("pt-BR")
        );

        assertThat(problem.getStatus()).isEqualTo(404);
        assertThat(problem.getTitle()).isEqualTo("Recurso não encontrado");
        assertThat(problem.getDetail()).isEqualTo("A solicitação de associação não foi encontrada.");
        assertThat(problem.getCode()).isEqualTo("MEMBERSHIP_APPLICATION_NOT_FOUND");
        assertThat(problem.getTimestamp()).isEqualTo(NOW);
    }

    @Test
    void createsProblemInEnglish() {
        ApiErrorResponse problem = problemFactory.create(
                HttpStatus.NOT_FOUND,
                "MEMBERSHIP_APPLICATION_NOT_FOUND",
                Locale.ENGLISH
        );

        assertThat(problem.getTitle()).isEqualTo("Resource not found");
        assertThat(problem.getDetail()).isEqualTo("The membership application was not found.");
        assertThat(problem.getCode()).isEqualTo("MEMBERSHIP_APPLICATION_NOT_FOUND");
        assertThat(problem.getTimestamp()).isEqualTo(NOW);
    }
}
