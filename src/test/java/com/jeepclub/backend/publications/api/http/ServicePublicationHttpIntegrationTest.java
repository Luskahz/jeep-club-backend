package com.jeepclub.backend.publications.api.http;

import com.jeepclub.backend.iam.authentication.core.application.service.security.AccessTokenAuthenticationService;
import com.jeepclub.backend.memberships.api.module.MembershipAccessQuery;
import com.jeepclub.backend.memberships.api.module.MembershipAccessResult;
import com.jeepclub.backend.platform.security.authorization.UserAuthoritiesProvider;
import com.jeepclub.backend.platform.security.jwt.JwtAuthenticatedUser;
import com.jeepclub.backend.platform.security.jwt.JwtTokenParser;
import com.jeepclub.backend.platform.storage.image.ImageMediaService;
import com.jeepclub.backend.publications.infra.persistence.entity.ServicePublicationHistoryEntity;
import com.jeepclub.backend.publications.infra.persistence.jpa.PublicationHistoryJpaRepository;
import com.jeepclub.backend.publications.infra.persistence.jpa.PublicationJpaRepository;
import com.jeepclub.backend.publications.infra.persistence.jpa.ServicePublicationChangeRequestJpaRepository;
import com.jeepclub.backend.publications.infra.persistence.jpa.ServicePublicationRequestJpaRepository;
import com.jeepclub.backend.shared.storage.exception.StorageObjectNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServicePublicationHttpIntegrationTest {
    private static final String KEY = "images/2026/09/27/550e8400-e29b-41d4-a716-446655440000.jpg";
    private static final String NEXT = "images/2026/09/27/550e8400-e29b-41d4-a716-446655440001.jpg";
    private static final String CREATE = """
            {"title":" Original ","content":" Body ","amount":100,"contactPhone":"123",
             "images":[{"storageKey":"%s","position":0,"primary":true}]}
            """;
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private PublicationJpaRepository publications;
    @Autowired private PublicationHistoryJpaRepository history;
    @Autowired private ServicePublicationRequestJpaRepository initialRequests;
    @Autowired private ServicePublicationChangeRequestJpaRepository changes;
    @MockitoBean private JwtTokenParser jwtTokenParser;
    @MockitoBean private UserAuthoritiesProvider authoritiesProvider;
    @MockitoBean private AccessTokenAuthenticationService authenticationService;
    @MockitoBean private MembershipAccessQuery membershipAccess;
    @MockitoBean private ImageMediaService media;

    @Test @Transactional
    void initialApprovalChangeApprovalAndOwnerDeleteKeepAuditAndPublishedIdentity() throws Exception {
        authenticate(99L, "PUBLICATIONS_SERVICE_REQUEST_CREATE");
        MvcResult created = mvc.perform(token(post("/service-publication-requests")
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE.formatted(KEY))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.requestedByUserId").value(99))
                .andExpect(jsonPath("$.status").value("PENDING")).andReturn();
        long requestId = id(created);
        verify(media).requireExisting(KEY);
        assertThat(publications.count()).isZero();

        authenticate(99L, "PUBLICATIONS_SERVICE_REQUEST_READ");
        mvc.perform(token(get("/service-publication-requests/{id}", requestId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Original"))
                .andExpect(jsonPath("$.reviewedByUserId").doesNotExist());
        authenticate(500L, "PUBLICATIONS_SERVICE_REQUEST_ADMIN_READ");
        mvc.perform(token(get("/admin/service-publication-requests?status=PENDING")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(requestId))
                .andExpect(jsonPath("$.totalElements").value(1));
        authenticate(500L, "PUBLICATIONS_SERVICE_REQUEST_APPROVE");
        MvcResult approval = mvc.perform(token(post("/admin/service-publication-requests/{id}/approve", requestId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.reviewedByUserId").value(500)).andReturn();
        long serviceId = json.readTree(approval.getResponse().getContentAsString()).get("createdPublicationId").asLong();
        assertThat(serviceId).isPositive();
        mvc.perform(token(post("/admin/service-publication-requests/{id}/approve", requestId)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SERVICE_REQUEST_ALREADY_PROCESSED"));

        authenticate(99L, "PUBLICATIONS_SERVICE_READ");
        MvcResult published = mvc.perform(token(get("/services/{id}", serviceId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.title").value("Original"))
                .andExpect(jsonPath("$.sourceRequestId").value(requestId)).andReturn();
        String publishedAt = json.readTree(published.getResponse().getContentAsString()).get("publishedAt").asString();

        authenticate(99L, "PUBLICATIONS_SERVICE_CHANGE_REQUEST_CREATE");
        MvcResult proposed = mvc.perform(token(patch("/services/{id}", serviceId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Revised\",\"amount\":150,\"images\":[{\"storageKey\":\"" + NEXT
                                + "\",\"position\":0,\"primary\":true}]}")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.proposedContent").value("Body"))
                .andExpect(jsonPath("$.proposedContactPhone").value("123")).andReturn();
        long changeId = id(proposed);
        verify(media).requireExisting(NEXT);
        authenticate(99L, "PUBLICATIONS_SERVICE_CHANGE_REQUEST_READ");
        mvc.perform(token(get("/service-publication-change-requests/{id}", changeId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.proposedTitle").value("Revised"))
                .andExpect(jsonPath("$.reviewedByUserId").doesNotExist());
        authenticate(99L, "PUBLICATIONS_SERVICE_CHANGE_REQUEST_CREATE");
        mvc.perform(token(patch("/services/{id}", serviceId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":180}")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SERVICE_CHANGE_REQUEST_ALREADY_PENDING"));
        authenticate(99L, "PUBLICATIONS_SERVICE_READ");
        mvc.perform(token(get("/services/{id}", serviceId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Original"))
                .andExpect(jsonPath("$.amount").value(100));

        authenticate(500L, "PUBLICATIONS_SERVICE_CHANGE_REQUEST_ADMIN_READ");
        mvc.perform(token(get("/admin/service-publication-change-requests?status=PENDING")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(changeId));
        mvc.perform(token(get("/admin/service-publication-change-requests/{id}", changeId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.proposedAmount").value(150));
        authenticate(500L, "PUBLICATIONS_SERVICE_CHANGE_REQUEST_APPROVE");
        mvc.perform(token(post("/admin/service-publication-change-requests/{id}/approve", changeId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
        mvc.perform(token(post("/admin/service-publication-change-requests/{id}/approve", changeId)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SERVICE_CHANGE_REQUEST_ALREADY_PROCESSED"));
        authenticate(99L, "PUBLICATIONS_SERVICE_READ");
        mvc.perform(token(get("/services/{id}", serviceId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(serviceId))
                .andExpect(jsonPath("$.title").value("Revised"))
                .andExpect(jsonPath("$.amount").value(150))
                .andExpect(jsonPath("$.publishedAt").value(publishedAt));

        authenticate(99L, "PUBLICATIONS_SERVICE_DELETE");
        mvc.perform(token(delete("/services/{id}", serviceId))).andExpect(status().isNoContent());
        assertThat(initialRequests.findById(requestId)).isPresent();
        assertThat(changes.findById(changeId)).isPresent();
        var snapshot = history.findAll().stream().filter(item -> item.getPublicationId().equals(serviceId))
                .map(ServicePublicationHistoryEntity.class::cast).findFirst().orElseThrow();
        assertThat(snapshot.getDeletedByUserId()).isEqualTo(99L);
        assertThat(snapshot.getTitle()).isEqualTo("Revised");
        assertThat(snapshot.getAmount()).isEqualByComparingTo("150.00");
        assertThat(snapshot.getSourceRequestId()).isEqualTo(requestId);
    }

    @Test @Transactional
    void rejectionOwnershipAndPatchValidationAreControlled() throws Exception {
        authenticate(99L, "PUBLICATIONS_SERVICE_REQUEST_CREATE");
        long initial = id(mvc.perform(token(post("/service-publication-requests")
                .contentType(MediaType.APPLICATION_JSON).content(CREATE.formatted(KEY)))).andExpect(status().isCreated()).andReturn());
        authenticate(88L, "PUBLICATIONS_SERVICE_REQUEST_READ");
        mvc.perform(token(get("/service-publication-requests/{id}", initial)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SERVICE_REQUEST_NOT_FOUND"));
        authenticate(500L, "PUBLICATIONS_SERVICE_REQUEST_REJECT");
        mvc.perform(token(post("/admin/service-publication-requests/{id}/reject", initial)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rejectionReason\":\" no \"}")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("no"));
        mvc.perform(token(post("/admin/service-publication-requests/{id}/reject", initial)
                        .contentType(MediaType.APPLICATION_JSON).content("{}")))
                .andExpect(status().isConflict());
        assertThat(publications.count()).isZero();

        authenticate(99L, "PUBLICATIONS_SERVICE_REQUEST_CREATE");
        long nextInitial = id(mvc.perform(token(post("/service-publication-requests")
                .contentType(MediaType.APPLICATION_JSON).content(CREATE.formatted(KEY)))).andExpect(status().isCreated()).andReturn());
        authenticate(500L, "PUBLICATIONS_SERVICE_REQUEST_APPROVE");
        long serviceId = json.readTree(mvc.perform(token(post("/admin/service-publication-requests/{id}/approve", nextInitial)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("createdPublicationId").asLong();
        authenticate(88L, "PUBLICATIONS_SERVICE_CHANGE_REQUEST_CREATE");
        mvc.perform(token(patch("/services/{id}", serviceId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Other\"}")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("SERVICE_NOT_OWNER"));
        authenticate(99L, "PUBLICATIONS_SERVICE_CHANGE_REQUEST_CREATE");
        for (String body : new String[]{"{}", "{\"title\":null}", "{\"amount\":null}",
                "{\"images\":null}", "{\"images\":[]}", "{\"unknown\":1}"}) {
            mvc.perform(token(patch("/services/{id}", serviceId).contentType(MediaType.APPLICATION_JSON).content(body)))
                    .andExpect(status().isBadRequest());
        }
        doThrow(new StorageObjectNotFoundException()).when(media).requireExisting(NEXT);
        mvc.perform(token(patch("/services/{id}", serviceId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"images\":[{\"storageKey\":\"" + NEXT + "\",\"position\":0,\"primary\":true}]}")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("IMAGE_NOT_FOUND"));
        authenticate(88L, "PUBLICATIONS_SERVICE_DELETE");
        mvc.perform(token(delete("/services/{id}", serviceId)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("SERVICE_NOT_OWNER"));
    }

    @Test @Transactional
    void rejectedChangeDoesNotModifyServiceAndAdminDeleteLeavesPendingChange() throws Exception {
        authenticate(99L, "PUBLICATIONS_SERVICE_REQUEST_CREATE");
        long requestId = id(mvc.perform(token(post("/service-publication-requests")
                .contentType(MediaType.APPLICATION_JSON).content(CREATE.formatted(KEY)))).andExpect(status().isCreated()).andReturn());
        authenticate(500L, "PUBLICATIONS_SERVICE_REQUEST_APPROVE");
        long serviceId = json.readTree(mvc.perform(token(post("/admin/service-publication-requests/{id}/approve", requestId)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("createdPublicationId").asLong();
        authenticate(99L, "PUBLICATIONS_SERVICE_CHANGE_REQUEST_CREATE");
        long rejectedId = id(mvc.perform(token(patch("/services/{id}", serviceId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Rejected title\"}"))).andExpect(status().isCreated()).andReturn());
        authenticate(500L, "PUBLICATIONS_SERVICE_CHANGE_REQUEST_REJECT");
        mvc.perform(token(post("/admin/service-publication-change-requests/{id}/reject", rejectedId)
                .contentType(MediaType.APPLICATION_JSON).content("{\"rejectionReason\":\"no\"}")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED"));
        mvc.perform(token(post("/admin/service-publication-change-requests/{id}/reject", rejectedId)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))).andExpect(status().isConflict());
        authenticate(99L, "PUBLICATIONS_SERVICE_READ");
        mvc.perform(token(get("/services/{id}", serviceId))).andExpect(jsonPath("$.title").value("Original"));
        authenticate(99L, "PUBLICATIONS_SERVICE_CHANGE_REQUEST_CREATE");
        long pendingId = id(mvc.perform(token(patch("/services/{id}", serviceId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Pending title\"}"))).andExpect(status().isCreated()).andReturn());
        authenticate(500L, "PUBLICATIONS_SERVICE_ADMIN_DELETE");
        authenticate(500L, "PUBLICATIONS_SERVICE_ADMIN_READ");
        mvc.perform(token(get("/admin/services/{id}", serviceId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Original"));
        authenticate(500L, "PUBLICATIONS_SERVICE_ADMIN_DELETE");
        mvc.perform(token(delete("/admin/services/{id}", serviceId))).andExpect(status().isNoContent());
        authenticate(500L, "PUBLICATIONS_SERVICE_CHANGE_REQUEST_APPROVE");
        mvc.perform(token(post("/admin/service-publication-change-requests/{id}/approve", pendingId)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SERVICE_NOT_FOUND"));
        assertThat(changes.findById(pendingId).orElseThrow().getStatus())
                .isEqualTo(com.jeepclub.backend.publications.core.domain.enums.ServicePublicationChangeRequestStatus.PENDING);
    }

    @Test void permissionsMembershipAndNoticeAuthorityAreIsolated() throws Exception {
        mvc.perform(post("/service-publication-requests").contentType(MediaType.APPLICATION_JSON).content(CREATE.formatted(KEY)))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/admin/service-publication-requests")).andExpect(status().isUnauthorized());
        authenticate(99L, "PUBLICATIONS_NOTICE_CREATE");
        var serviceActions = new org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder[]{
                post("/service-publication-requests").contentType(MediaType.APPLICATION_JSON).content(CREATE.formatted(KEY)),
                get("/service-publication-requests/1"),
                get("/admin/service-publication-requests"),
                get("/admin/service-publication-requests/1"),
                post("/admin/service-publication-requests/1/approve"),
                post("/admin/service-publication-requests/1/reject").contentType(MediaType.APPLICATION_JSON).content("{}"),
                get("/services/1"),
                patch("/services/1").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Next\"}"),
                delete("/services/1"),
                get("/service-publication-change-requests/1"),
                get("/admin/service-publication-change-requests"),
                get("/admin/service-publication-change-requests/1"),
                post("/admin/service-publication-change-requests/1/approve"),
                post("/admin/service-publication-change-requests/1/reject").contentType(MediaType.APPLICATION_JSON).content("{}"),
                get("/admin/services/1"),
                delete("/admin/services/1")
        };
        for (var action : serviceActions) mvc.perform(token(action)).andExpect(status().isForbidden());
        authenticate(99L, "PUBLICATIONS_SERVICE_REQUEST_READ");
        mvc.perform(token(post("/admin/service-publication-requests/1/approve"))).andExpect(status().isForbidden());
        authenticate(500L, "PUBLICATIONS_SERVICE_CHANGE_REQUEST_APPROVE");
        mvc.perform(token(post("/admin/service-publication-change-requests/1/reject")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))).andExpect(status().isForbidden());
        authenticate(99L, "PUBLICATIONS_SERVICE_REQUEST_CREATE");
        when(membershipAccess.evaluate(anyLong())).thenReturn(MembershipAccessResult.PAYMENT_REQUIRED);
        mvc.perform(token(post("/service-publication-requests").contentType(MediaType.APPLICATION_JSON).content(CREATE.formatted(KEY))))
                .andExpect(status().isPaymentRequired());
    }

    @Test @Transactional
    void initialPayloadAndMediaFailuresDoNotPersistRequests() throws Exception {
        authenticate(99L, "PUBLICATIONS_SERVICE_REQUEST_CREATE");
        for (String body : new String[]{"{}", "{\"title\":\" \"}",
                CREATE.formatted(KEY).replace("\"title\"", "\"requestedByUserId\":88,\"title\""),
                CREATE.formatted(KEY).replace("\"amount\":100", "\"amount\":0"),
                "{\"title\":\"A\",\"content\":\"B\",\"amount\":100,\"contactPhone\":\"123\",\"images\":[]}"}) {
            mvc.perform(token(post("/service-publication-requests").contentType(MediaType.APPLICATION_JSON).content(body)))
                    .andExpect(status().isBadRequest());
        }
        assertThat(initialRequests.count()).isZero();
        doThrow(new StorageObjectNotFoundException()).when(media).requireExisting(KEY);
        mvc.perform(token(post("/service-publication-requests").contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE.formatted(KEY))))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("IMAGE_NOT_FOUND"));
        assertThat(initialRequests.count()).isZero();
    }

    private void authenticate(long userId, String... permissions) {
        when(jwtTokenParser.parseAndValidate("service-token")).thenReturn(
                new JwtAuthenticatedUser(userId, userId + 1000, "Service User", Instant.now().plusSeconds(3600)));
        when(authoritiesProvider.findAuthorityCodesByUserId(userId)).thenReturn(List.of(permissions));
        when(membershipAccess.evaluate(anyLong())).thenReturn(MembershipAccessResult.ALLOWED);
    }
    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder token(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request) {
        return request.header(AUTHORIZATION, "Bearer service-token");
    }
    private long id(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }
}
