package com.jeepclub.backend.publications.api.http;

import com.jeepclub.backend.iam.authentication.core.application.service.security.AccessTokenAuthenticationService;
import com.jeepclub.backend.platform.security.authorization.UserAuthoritiesProvider;
import com.jeepclub.backend.platform.security.jwt.JwtAuthenticatedUser;
import com.jeepclub.backend.platform.security.jwt.JwtTokenParser;
import com.jeepclub.backend.platform.storage.image.ImageMediaService;
import com.jeepclub.backend.publications.infra.persistence.entity.NoticeHistoryEntity;
import com.jeepclub.backend.publications.infra.persistence.jpa.PublicationHistoryJpaRepository;
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
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NoticeHttpIntegrationTest {
    private static final String KEY = "images/2026/09/27/550e8400-e29b-41d4-a716-446655440000.jpg";
    private static final String NEXT = "images/2026/09/27/550e8400-e29b-41d4-a716-446655440001.jpg";
    private static final String CREATE = """
            {"title":" First notice ","content":" Body ","images":[{"storageKey":"%s","position":0,"primary":true}]}
            """;
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private PublicationHistoryJpaRepository history;
    @MockitoBean private JwtTokenParser jwtTokenParser;
    @MockitoBean private UserAuthoritiesProvider authoritiesProvider;
    @MockitoBean private AccessTokenAuthenticationService authenticationService;
    @MockitoBean private ImageMediaService media;

    @Test
    void everyNoticeActionRequiresAuthenticationAndItsOwnAuthority() throws Exception {
        RequestBuilder[] actions = {
                post("/admin/notices").contentType(MediaType.APPLICATION_JSON).content(CREATE.formatted(KEY)),
                get("/admin/notices/1"),
                patch("/admin/notices/1").contentType(MediaType.APPLICATION_JSON).content("{}"),
                post("/admin/notices/1/publish"),
                post("/admin/notices/1/archive"),
                delete("/admin/notices/1")
        };
        for (RequestBuilder action : actions) {
            mvc.perform(action).andExpect(status().isUnauthorized());
        }
        authenticate(List.of("PUBLICATIONS_EVENT_CREATE", "PUBLICATIONS_NOTICE_READ"));
        // READ alone cannot create, update, publish, archive or delete Notice.
        for (int index : new int[]{0, 2, 3, 4, 5}) {
            mvc.perform(withToken(actions[index])).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        }
        authenticate(List.of("PUBLICATIONS_NOTICE_CREATE"));
        mvc.perform(withToken(get("/admin/notices/1"))).andExpect(status().isForbidden());
        mvc.perform(withToken(delete("/admin/notices/1"))).andExpect(status().isForbidden());
        authenticate(List.of("PUBLICATIONS_NOTICE_UPDATE"));
        mvc.perform(withToken(post("/admin/notices/1/publish"))).andExpect(status().isForbidden());
        authenticate(List.of("PUBLICATIONS_NOTICE_PUBLISH"));
        mvc.perform(withToken(post("/admin/notices/1/archive"))).andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void eachPermissionRunsOnlyItsActionThroughJoinedPersistenceAndDeleteHistory() throws Exception {
        authenticate(List.of("PUBLICATIONS_NOTICE_CREATE"));
        MvcResult created = mvc.perform(withToken(post("/admin/notices")
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE.formatted(KEY))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorUserId").value(99))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.title").value("First notice"))
                .andExpect(jsonPath("$.images[0].storageKey").value(KEY))
                .andReturn();
        long id = json.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        verify(media).requireExisting(KEY);

        authenticate(List.of("PUBLICATIONS_NOTICE_READ"));
        mvc.perform(withToken(get("/admin/notices/{id}", id)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DRAFT"));

        authenticate(List.of("PUBLICATIONS_NOTICE_UPDATE"));
        mvc.perform(withToken(patch("/admin/notices/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Revised\"}")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Revised"))
                .andExpect(jsonPath("$.content").value("Body"));
        mvc.perform(withToken(patch("/admin/notices/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"images\":[{\"storageKey\":\"" + NEXT
                                + "\",\"position\":0,\"primary\":true}]}")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.images[0].storageKey").value(NEXT));
        verify(media).requireExisting(NEXT);

        authenticate(List.of("PUBLICATIONS_NOTICE_PUBLISH"));
        MvcResult published = mvc.perform(withToken(post("/admin/notices/{id}/publish", id)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andReturn();
        String publishedAt = json.readTree(published.getResponse().getContentAsString()).get("publishedAt").asString();
        mvc.perform(withToken(post("/admin/notices/{id}/publish", id)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("NOTICE_INVALID_STATE"));

        authenticate(List.of("PUBLICATIONS_NOTICE_UPDATE"));
        mvc.perform(withToken(patch("/admin/notices/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"Published edit\"}")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.publishedAt").value(publishedAt))
                .andExpect(jsonPath("$.content").value("Published edit"));

        authenticate(List.of("PUBLICATIONS_NOTICE_ARCHIVE"));
        mvc.perform(withToken(post("/admin/notices/{id}/archive", id)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ARCHIVED"));
        mvc.perform(withToken(post("/admin/notices/{id}/archive", id)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("NOTICE_INVALID_STATE"));

        authenticate(List.of("PUBLICATIONS_NOTICE_UPDATE"));
        mvc.perform(withToken(patch("/admin/notices/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"No\"}")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("NOTICE_INVALID_STATE"));

        authenticate(List.of("PUBLICATIONS_NOTICE_DELETE"));
        mvc.perform(withToken(delete("/admin/notices/{id}", id))).andExpect(status().isNoContent());
        var snapshot = history.findAll().stream().filter(item -> item.getPublicationId().equals(id))
                .map(NoticeHistoryEntity.class::cast).findFirst().orElseThrow();
        assertThat(snapshot.getDeletedByUserId()).isEqualTo(99L);
        assertThat(snapshot.getTitle()).isEqualTo("Revised");
        assertThat(snapshot.getContent()).isEqualTo("Published edit");
        assertThat(snapshot.getStatus().name()).isEqualTo("ARCHIVED");
        assertThat(snapshot.getImages()).singleElement().satisfies(image -> assertThat(image.getStorageKey()).isEqualTo(NEXT));
        authenticate(List.of("PUBLICATIONS_NOTICE_READ"));
        mvc.perform(withToken(get("/admin/notices/{id}", id)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOTICE_NOT_FOUND"));
    }

    @Test
    @Transactional
    void invalidPayloadMissingMediaAndMissingIdsReturnControlledProblems() throws Exception {
        authenticate(List.of("PUBLICATIONS_NOTICE_CREATE"));
        mvc.perform(withToken(post("/admin/notices").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\" \"}")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOTICE_INVALID_REQUEST"));
        mvc.perform(withToken(post("/admin/notices").contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE.formatted(KEY).replace("\"title\"", "\"authorUserId\":123,\"title\""))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("NOTICE_INVALID_REQUEST"));
        doThrow(new StorageObjectNotFoundException()).when(media).requireExisting(KEY);
        mvc.perform(withToken(post("/admin/notices").contentType(MediaType.APPLICATION_JSON).content(CREATE.formatted(KEY))))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("IMAGE_NOT_FOUND"));

        authenticate(List.of("PUBLICATIONS_NOTICE_READ"));
        mvc.perform(withToken(get("/admin/notices/987654321")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOTICE_NOT_FOUND"));
        authenticate(List.of("PUBLICATIONS_NOTICE_UPDATE"));
        mvc.perform(withToken(patch("/admin/notices/987654321").contentType(MediaType.APPLICATION_JSON).content("{}")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOTICE_NOT_FOUND"));
        authenticate(List.of("PUBLICATIONS_NOTICE_PUBLISH"));
        mvc.perform(withToken(post("/admin/notices/987654321/publish"))).andExpect(status().isNotFound());
        authenticate(List.of("PUBLICATIONS_NOTICE_ARCHIVE"));
        mvc.perform(withToken(post("/admin/notices/987654321/archive"))).andExpect(status().isNotFound());
        authenticate(List.of("PUBLICATIONS_NOTICE_DELETE"));
        mvc.perform(withToken(delete("/admin/notices/987654321"))).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void patchDistinguishesOmittedFieldsFromExplicitNullAndValidatesReplacementGallery() throws Exception {
        authenticate(List.of("PUBLICATIONS_NOTICE_CREATE"));
        MvcResult created = mvc.perform(withToken(post("/admin/notices")
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE.formatted(KEY))))
                .andExpect(status().isCreated()).andReturn();
        long id = json.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        String initialUpdatedAt = json.readTree(created.getResponse().getContentAsString()).get("updatedAt").asString();

        authenticate(List.of("PUBLICATIONS_NOTICE_UPDATE"));
        mvc.perform(withToken(patch("/admin/notices/{id}", id).contentType(MediaType.APPLICATION_JSON).content("{}")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.updatedAt").value(initialUpdatedAt))
                .andExpect(jsonPath("$.images[0].storageKey").value(KEY));
        for (String body : new String[]{"{\"title\":null}", "{\"content\":null}", "{\"images\":null}",
                "{\"images\":[]}", "{\"authorUserId\":123}"}) {
            mvc.perform(withToken(patch("/admin/notices/{id}", id).contentType(MediaType.APPLICATION_JSON).content(body)))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(withToken(patch("/admin/notices/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"images\":[{\"storageKey\":\"" + NEXT + "\",\"position\":0,\"primary\":false}]}")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("NOTICE_INVALID_REQUEST"));
        doThrow(new StorageObjectNotFoundException()).when(media).requireExisting(NEXT);
        mvc.perform(withToken(patch("/admin/notices/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"images\":[{\"storageKey\":\"" + NEXT + "\",\"position\":0,\"primary\":true}]}")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("IMAGE_NOT_FOUND"));
        authenticate(List.of("PUBLICATIONS_NOTICE_READ"));
        mvc.perform(withToken(get("/admin/notices/{id}", id)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.images[0].storageKey").value(KEY));
    }

    private void authenticate(List<String> authorities) {
        when(jwtTokenParser.parseAndValidate("notice-token")).thenReturn(
                new JwtAuthenticatedUser(99L, 199L, "Notice Admin", Instant.now().plusSeconds(3600)));
        when(authoritiesProvider.findAuthorityCodesByUserId(99L)).thenReturn(authorities);
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder withToken(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request) {
        return request.header(AUTHORIZATION, "Bearer notice-token");
    }

    private static RequestBuilder withToken(RequestBuilder request) {
        return ((org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder) request)
                .header(AUTHORIZATION, "Bearer notice-token");
    }
}
