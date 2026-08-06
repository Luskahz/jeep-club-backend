package com.jeepclub.backend.platform.web.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ApiProblemResponseWriter {

    private final ApiProblemFactory problemFactory;
    private final ObjectMapper objectMapper;
    private final LocaleResolver localeResolver;

    public void write(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String code
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
                response.getOutputStream(),
                problemFactory.create(
                        status,
                        code,
                        localeResolver.resolveLocale(request)
                )
        );
    }
}
