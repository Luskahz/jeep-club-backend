package com.jeepclub.backend.publications.api.http;

import com.jeepclub.backend.iam.authentication.core.application.service.security.AccessTokenAuthenticationService;
import com.jeepclub.backend.memberships.api.module.*;
import com.jeepclub.backend.platform.security.authorization.UserAuthoritiesProvider;
import com.jeepclub.backend.platform.security.jwt.*;
import com.jeepclub.backend.platform.storage.image.ImageMediaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class EventHttpIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired Clock clock;
    @MockitoBean JwtTokenParser jwtTokenParser;
    @MockitoBean UserAuthoritiesProvider authoritiesProvider;
    @MockitoBean AccessTokenAuthenticationService authenticationService;
    @MockitoBean MembershipAccessQuery membershipAccess;
    @MockitoBean ImageMediaService media;
    static final String CREATE = """
        {"title":"Trail","content":"Route","startsAt":"%s","images":[{"storageKey":"images/2026/09/27/550e8400-e29b-41d4-a716-446655440000.jpg","position":0,"primary":true}]}
        """;
    void auth(long user, String... permissions) {
        when(jwtTokenParser.parseAndValidate("event-token")).thenReturn(new JwtAuthenticatedUser(user, user + 1000, "Event user", Instant.now(clock).plusSeconds(3600)));
        when(authoritiesProvider.findAuthorityCodesByUserId(user)).thenReturn(List.of(permissions));
        when(membershipAccess.evaluate(anyLong())).thenReturn(MembershipAccessResult.ALLOWED);
    }
    static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder token(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder r) {
        return r.header("Authorization", "Bearer event-token");
    }
    @Test @Transactional void fullHttpCreationPatchRegistrationAndControlledErrors() throws Exception {
        auth(99L, "PUBLICATIONS_EVENT_CREATE");
        String body = CREATE.formatted(Instant.now(clock).plusSeconds(86400));
        var result = mvc.perform(token(post("/admin/events").contentType("application/json").content(body)))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.eventStatus").value("OPEN"))
            .andExpect(jsonPath("$.publicationStatus").value("DRAFT")).andExpect(jsonPath("$.authorUserId").value(99)).andReturn();
        long id = json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        auth(99L, "PUBLICATIONS_EVENT_UPDATE");
        mvc.perform(token(patch("/admin/events/{id}", id).contentType("application/json").content("{\"title\":\"Revised\"}")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content").value("Route"));
        for (String invalid : List.of("{\"startsAt\":null}", "{\"title\":null}", "{\"charges\":null}", "{\"charges\":[{\"recurrenceType\":\"MONTHLY\"}]}", "{\"authorUserId\":12}")) {
            mvc.perform(token(patch("/admin/events/{id}", id).contentType("application/json").content(invalid)))
                .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        }
        auth(99L, "PUBLICATIONS_EVENT_PUBLISH");
        mvc.perform(token(post("/admin/events/{id}/publish", id))).andExpect(status().isOk());
        auth(10L, "PUBLICATIONS_EVENT_REGISTER", "PUBLICATIONS_EVENT_REGISTRATION_READ");
        mvc.perform(token(post("/events/{id}/registrations", id).contentType("application/json").content("{\"allocations\":[]}")))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("CONFIRMED")).andExpect(jsonPath("$.userId").value(10));
        mvc.perform(token(post("/events/{id}/registrations", id).contentType("application/json").content("{\"allocations\":[]}")))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EVENT_ALREADY_REGISTERED"));
        auth(11L, "PUBLICATIONS_EVENT_REGISTRATION_READ");
        mvc.perform(token(get("/events/{id}/registrations/me", id))).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("EVENT_REGISTRATION_NOT_FOUND"));
    }
    @Test void endpointPermissionsAreEnforcedIncludingEmergencyHealth() throws Exception {
        var actions = List.of(get("/admin/events/1"), post("/admin/events/1/publish"), post("/admin/events/1/cancel"),
            post("/admin/events/1/finish"), delete("/admin/events/1"), get("/admin/events/1/dashboard"),
            get("/admin/events/1/health/USER/10"), get("/admin/events/1/guest-requests"), get("/admin/events/1/ride-offers"),
            post("/admin/events/1/ride-offers/1/select"), post("/admin/events/1/guest-requests/1/approve"),
            get("/events/1"), get("/events/1/registrations/me"), get("/events/1/ride-requests"), get("/events/1/guest-requests"));
        for (var action : actions) mvc.perform(action).andExpect(status().isUnauthorized());
        auth(99L, "PUBLICATIONS_NOTICE_READ");
        for (var action : actions) mvc.perform(token(action)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        auth(99L, "PUBLICATIONS_EVENT_HEALTH_EMERGENCY_READ");
        mvc.perform(token(get("/admin/events/1/health/USER/10"))).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"));
        auth(10L, "PUBLICATIONS_EVENT_READ");
        when(membershipAccess.evaluate(10L)).thenReturn(MembershipAccessResult.PAYMENT_REQUIRED);
        mvc.perform(token(get("/events/1"))).andExpect(status().isPaymentRequired());
    }
    @Test @Transactional void missingChargeUsesControlledProblemAndEditorsCanReadCatalog() throws Exception {
        auth(99L, "PUBLICATIONS_EVENT_UPDATE");
        mvc.perform(token(get("/admin/events/charge-catalog"))).andExpect(status().isOk());
        auth(99L, "PUBLICATIONS_EVENT_CREATE");
        String body = CREATE.formatted(Instant.now(clock).plusSeconds(86400)).trim();
        body = body.substring(0, body.length() - 1) + ",\"charges\":[{\"chargeDefinitionId\":9223372036854775807,\"requiredForParticipation\":true}]}";
        mvc.perform(token(post("/admin/events").contentType("application/json").content(body)))
            .andExpect(status().isNotFound()).andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.code").value("EVENT_CHARGE_NOT_FOUND"));
    }
    @Test @Transactional void inlineHttpChargeDefaultsOptionalFlagsAndReturnsRule() throws Exception {
        auth(99L, "PUBLICATIONS_EVENT_CREATE", "PUBLICATIONS_EVENT_READ_ADMIN");
        String body = CREATE.formatted(Instant.now(clock).plusSeconds(86400)).trim();
        body = body.substring(0, body.length() - 1) + ",\"charges\":[{\"name\":\"HTTP-" + UUID.randomUUID() + "\",\"amount\":20}]}";
        var result = mvc.perform(token(post("/admin/events").contentType("application/json").content(body)))
            .andExpect(status().isCreated()).andReturn();
        long id = json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(token(get("/admin/events/{id}/charges",id))).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].chargeDefinitionId").isNumber())
            .andExpect(jsonPath("$[0].requiredForParticipation").value(false));
    }
    @Test @Transactional void httpAcceptsFinancialDueDateAndExplicitNullAndRejectsInvalidDate() throws Exception {
        auth(99L, "PUBLICATIONS_EVENT_CREATE", "PUBLICATIONS_EVENT_READ_ADMIN");
        for (String date : List.of("\"2026-11-30\"", "null")) {
            String body = CREATE.formatted(Instant.now(clock).plusSeconds(86400)).trim();
            body = body.substring(0, body.length() - 1) + ",\"charges\":[{\"name\":\"HTTP-" + UUID.randomUUID()
                + "\",\"amount\":20,\"financialDueDate\":" + date + "}]}";
            var result = mvc.perform(token(post("/admin/events").contentType("application/json").content(body)))
                .andExpect(status().isCreated()).andReturn();
            long id = json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
            mvc.perform(token(get("/admin/events/{id}/charges", id))).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].financialDueDate").value(date.equals("null") ? null : "2026-11-30"));
        }
        String invalid = CREATE.formatted(Instant.now(clock).plusSeconds(86400)).trim();
        invalid = invalid.substring(0, invalid.length() - 1) + ",\"charges\":[{\"name\":\"Invalid date\",\"amount\":20,\"financialDueDate\":\"30/11/2026\"}]}";
        mvc.perform(token(post("/admin/events").contentType("application/json").content(invalid)))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("EVENT_INVALID_REQUEST"));
    }
    @Test void openApiDescribesEveryEventOperationAndTypedCatalogPagination() throws Exception {
        var result = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        var document = json.readTree(result.getResponse().getContentAsString());
        int checked = 0;
        for (String path : document.get("paths").propertyNames()) {
            if (!path.startsWith("/events") && !path.startsWith("/admin/events")) continue;
            var item = document.get("paths").get(path);
            for (String method : item.propertyNames()) {
                var operation = item.get(method);
                String permission = operation.get("x-required-permissions").get(0).asString();
                if (path.endsWith("/export")) {
                    assertThat(permission).isEqualTo(path.contains("/health/")
                        ? "PUBLICATIONS_EVENT_HEALTH_EMERGENCY_READ" : "PUBLICATIONS_EXPORT");
                    var content = operation.get("responses").get("200").get("content");
                    assertThat(content.has("application/pdf")).isTrue();
                    if (!path.contains("/health/")) assertThat(content.has("text/csv")).isTrue();
                } else {
                    assertThat(permission).startsWith("PUBLICATIONS_EVENT_");
                    assertThat(operation.get("responses").get("409").get("content").has("application/problem+json")).isTrue();
                }
                assertThat(operation.get("responses").has("403")).isTrue();
                checked++;
            }
        }
        assertThat(checked).isGreaterThanOrEqualTo(25);
        assertThat(document.get("paths").get("/admin/events/charge-catalog").get("get").get("responses").get("200").get("content").get("*/*").get("schema").get("$ref").asString()).contains("PageResponse");
        assertThat(document.get("components").get("schemas").get("EventResponseDTO").get("properties").get("eventStatus").get("enum").toString()).contains("OPEN", "FINISHED", "CANCELLED");
        assertThat(document.get("components").get("schemas").get("Charge").get("properties").get("financialDueDate").get("format").asString()).isEqualTo("date");
    }
}
