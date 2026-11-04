package com.jeepclub.backend.platform.security.handler;

import com.jeepclub.backend.platform.web.exception.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import tools.jackson.databind.json.JsonMapper;
import java.time.*;
import static org.assertj.core.api.Assertions.*;

class ApiAccessDeniedHandlerTest {
    @Test void realProblemWriterReturns403WithoutExceptionOrRequestSecrets() throws Exception {
        var messages = new ResourceBundleMessageSource();
        messages.setBasename("messages"); messages.setDefaultEncoding("UTF-8");
        var json = JsonMapper.builder().build();
        var writer = new ApiProblemResponseWriter(new ApiProblemFactory(messages,
                Clock.fixed(Instant.parse("2026-10-01T12:00:00Z"),ZoneOffset.UTC)),json,new AcceptHeaderLocaleResolver());
        var request = new MockHttpServletRequest("GET","/synthetic");
        request.addHeader("Authorization","Bearer synthetic-secret");
        var response = new MockHttpServletResponse();
        new ApiAccessDeniedHandler(writer).handle(request,response,
                new AccessDeniedException("synthetic-private-root secret-token"));
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith("application/problem+json");
        var body = response.getContentAsString();
        assertThat(json.readTree(body).path("status").asInt()).isEqualTo(403);
        assertThat(json.readTree(body).path("code").asString()).isEqualTo("ACCESS_DENIED");
        assertThat(json.readTree(body).path("detail").asString()).isNotBlank();
        assertThat(body).doesNotContain("synthetic-private-root","secret-token","synthetic-secret","stackTrace");
    }
}
