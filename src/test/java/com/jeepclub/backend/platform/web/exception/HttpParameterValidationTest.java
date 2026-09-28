package com.jeepclub.backend.platform.web.exception;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HttpParameterValidationTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        JsonMapper jsonMapper = JsonMapper.builder()
                .findAndAddModules()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();

        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(jsonMapper))
                .setValidator(validator)
                .build();
    }

    @Test
    void mvcMethodValidationMapsPathAndQueryConstraintsToBadRequest() throws Exception {
        mockMvc.perform(get("/test-validation/path/{id}", 0))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(HandlerMethodValidationException.class));

        for (String value : new String[] {"1", "6"}) {
            mockMvc.perform(get("/test-validation/query").param("size", value))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(result -> assertThat(result.getResolvedException())
                            .isInstanceOf(HandlerMethodValidationException.class));
        }

        mockMvc.perform(get("/test-validation/positive-query").param("id", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void bodyValidationStillReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/test-validation/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void domainConstraintViolationAndUnexpectedExceptionRemainInternalErrors() throws Exception {
        mockMvc.perform(get("/test-validation/domain-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"));
        mockMvc.perform(get("/test-validation/unexpected-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"));
    }

    @RestController
    static class TestController {

        @GetMapping("/test-validation/path/{id}")
        String path(@PathVariable @Positive Long id) {
            return "ok";
        }

        @GetMapping("/test-validation/query")
        String query(@RequestParam @Min(2) @Max(5) int size) {
            return "ok";
        }

        @GetMapping("/test-validation/positive-query")
        String positiveQuery(@RequestParam @Positive Long id) {
            return "ok";
        }

        @PostMapping("/test-validation/body")
        String body(@Valid @RequestBody Body body) {
            return "ok";
        }

        @GetMapping("/test-validation/domain-error")
        String domainError() {
            try (var factory = Validation.buildDefaultValidatorFactory()) {
                var violations = factory.getValidator().validate(new Body(""));
                throw new ConstraintViolationException(violations);
            }
        }

        @GetMapping("/test-validation/unexpected-error")
        String unexpectedError() {
            throw new RuntimeException("unexpected");
        }
    }

    record Body(@NotBlank String name) {}
}
