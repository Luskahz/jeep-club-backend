package com.jeepclub.backend.platform.security.filter;

import com.jeepclub.backend.platform.security.jwt.JwtProperties;
import com.jeepclub.backend.platform.web.exception.ApiProblemResponseWriter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FrontendAccessFilterTest {
    private final ApiProblemResponseWriter writer = mock(ApiProblemResponseWriter.class);
    private final FilterChain chain = mock(FilterChain.class);
    private final FrontendAccessFilter filter = new FrontendAccessFilter(true, "private-test-key", false,
            new JwtProperties(), writer);

    @Test
    void copiedOriginAndEvenBearerTokenDoNotAuthenticateTheFrontend() throws Exception {
        var request = new MockHttpServletRequest("POST", "/authentication/login");
        request.addHeader("Origin", "https://jeepclubetamoios-one.vercel.app");
        request.addHeader("Authorization", "Bearer user-token");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        verify(writer).write(request, response, HttpStatus.FORBIDDEN, "ACCESS_DENIED");
        verifyNoInteractions(chain);
    }

    @Test
    void wrongAndDuplicateKeysAreRejectedIncludingPreflight() throws Exception {
        for (boolean duplicate : new boolean[]{false, true}) {
            var request = new MockHttpServletRequest("OPTIONS", "/identity/me");
            request.addHeader(FrontendAccessFilter.HEADER, duplicate ? "private-test-key" : "wrong");
            if (duplicate) request.addHeader(FrontendAccessFilter.HEADER, "private-test-key");
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, chain);
            verify(writer).write(request, response, HttpStatus.FORBIDDEN, "ACCESS_DENIED");
        }
        verifyNoInteractions(chain);
    }

    @Test
    void trustedFrontendContinuesToUserAuthentication() throws Exception {
        var request = new MockHttpServletRequest("GET", "/identity/me");
        request.addHeader(FrontendAccessFilter.HEADER, "private-test-key");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verifyNoInteractions(writer);
    }

    @Test
    void documentationAndCustomStaticDocumentationReturn404EvenForFrontend() throws Exception {
        for (String path : new String[]{"/swagger-ui/index.html", "/swagger-ui.html", "/v3/api-docs",
                "/v3/api-docs/swagger-config", "/openapi-custom/index.html", "/docs/index.html"}) {
            var request = new MockHttpServletRequest("GET", path);
            request.addHeader(FrontendAccessFilter.HEADER, "private-test-key");
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, chain);
            assertEquals(404, response.getStatus(), path);
        }
        verifyNoInteractions(chain, writer);
    }

    @Test
    void healthExemptionRequiresLoopbackSeparatePortAndGet() throws Exception {
        var request = new MockHttpServletRequest("GET", "/actuator/health");
        request.setRemoteAddr("127.0.0.1");
        request.setLocalPort(8081);
        assertTrue(FrontendAccessFilter.isInternalHealth(request));
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        request.setLocalPort(8080);
        assertFalse(FrontendAccessFilter.isInternalHealth(request));
        request.setLocalPort(8081);
        request.setRemoteAddr("198.51.100.1");
        assertFalse(FrontendAccessFilter.isInternalHealth(request));
        request.setRemoteAddr("127.0.0.1");
        request.setMethod("POST");
        assertFalse(FrontendAccessFilter.isInternalHealth(request));
    }

    @Test
    void derivedCredentialIsPurposeSeparatedAndRequiresASecret() {
        String derived = FrontendAccessFilter.deriveKey("test-root-secret");
        assertEquals(43, derived.length());
        assertNotEquals("test-root-secret", derived);
        assertNotEquals(derived, FrontendAccessFilter.deriveKey("rotated-test-root-secret"));
        assertThrows(IllegalStateException.class, () -> FrontendAccessFilter.deriveKey(""));
    }
}
