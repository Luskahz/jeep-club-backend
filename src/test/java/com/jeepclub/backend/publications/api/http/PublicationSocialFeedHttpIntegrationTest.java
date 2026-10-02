package com.jeepclub.backend.publications.api.http;

import com.jeepclub.backend.iam.authentication.core.application.service.security.AccessTokenAuthenticationService;
import com.jeepclub.backend.memberships.api.module.*;
import com.jeepclub.backend.platform.security.authorization.UserAuthoritiesProvider;
import com.jeepclub.backend.platform.security.jwt.JwtAuthenticatedUser;
import com.jeepclub.backend.platform.security.jwt.JwtTokenParser;
import com.jeepclub.backend.platform.storage.image.ImageMediaService;
import com.jeepclub.backend.publications.core.domain.model.*;
import com.jeepclub.backend.publications.core.repository.PublicationRepository;
import com.jeepclub.backend.publications.core.repository.ServicePublicationRequestRepository;
import com.jeepclub.backend.publications.core.application.service.AdminServicePublicationRequestService;
import com.jeepclub.backend.shared.storage.exception.StorageObjectNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicationSocialFeedHttpIntegrationTest {
    private static final String KEY = "images/2026/09/27/550e8400-e29b-41d4-a716-446655440000.jpg";
    private static final Instant NOW = Instant.parse("2026-09-27T12:00:00Z");
    @Autowired private MockMvc mvc;
    @Autowired private PublicationRepository publications;
    @Autowired private ServicePublicationRequestRepository requests;
    @Autowired private AdminServicePublicationRequestService adminRequests;
    @MockitoBean private JwtTokenParser jwt;
    @MockitoBean private UserAuthoritiesProvider authorities;
    @MockitoBean private AccessTokenAuthenticationService authentication;
    @MockitoBean private MembershipAccessQuery membership;
    @MockitoBean private ImageMediaService media;

    @Test @Transactional void publishedNoticeSupportsLikeCommentAndFeedSummaryForTwoMembers() throws Exception {
        var notice = Notice.create(7L, "Trail notice", "Body", List.of(new PublicationImage(KEY, 0, true)), NOW);
        notice.publish(NOW.plusSeconds(1));
        long id = publications.save(notice).getId();
        var event = Event.create(7L, "Trail event", "Body", List.of(new PublicationImage(KEY, 0, true)),
                NOW.plusSeconds(3600), NOW);
        event.publish(NOW.plusSeconds(1));
        long eventId = publications.save(event).getId();
        var request = requests.save(ServicePublicationRequest.create(7L, "Trail service", "Body",
                new BigDecimal("12.00"), "123456789", List.of(new PublicationImage(KEY, 0, true)), NOW));
        long serviceId = adminRequests.approve(request.getId(), 8L).getCreatedPublicationId();
        when(membership.evaluate(anyLong())).thenReturn(MembershipAccessResult.ALLOWED);
        authenticate(9L);
        mvc.perform(post("/publications/{id}/likes", id).header(AUTHORIZATION, "Bearer social-token"))
                .andExpect(status().isOk());
        mvc.perform(post("/publications/{id}/likes", id).header(AUTHORIZATION, "Bearer social-token"))
                .andExpect(status().isOk());
        mvc.perform(post("/publications/{id}/comments", id).header(AUTHORIZATION, "Bearer social-token")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"Hello\",\"images\":[]}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/publications/{id}/comments", eventId).header(AUTHORIZATION, "Bearer social-token")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"Event discussion\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/publications/{id}/likes", serviceId).header(AUTHORIZATION, "Bearer social-token"))
                .andExpect(status().isOk());
        mvc.perform(get("/publications/feed").header(AUTHORIZATION, "Bearer social-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id==" + id + ")].type").value(org.hamcrest.Matchers.hasItem("NOTICE")))
                .andExpect(jsonPath("$.content[?(@.id==" + eventId + ")].type").value(org.hamcrest.Matchers.hasItem("EVENT")))
                .andExpect(jsonPath("$.content[?(@.id==" + serviceId + ")].type").value(org.hamcrest.Matchers.hasItem("SERVICE")))
                .andExpect(jsonPath("$.content[?(@.id==" + serviceId + ")].likedByMe").value(org.hamcrest.Matchers.hasItem(true)));
        mvc.perform(get("/publications/feed").header(AUTHORIZATION, "Bearer social-token").param("type", "NOTICE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id==" + id + ")].likeCount").value(org.hamcrest.Matchers.hasItem(1)))
                .andExpect(jsonPath("$.content[?(@.id==" + id + ")].commentCount").value(org.hamcrest.Matchers.hasItem(1)))
                .andExpect(jsonPath("$.content[?(@.id==" + id + ")].likedByMe").value(org.hamcrest.Matchers.hasItem(true)));
        mvc.perform(get("/publications/{id}/comments", id).header(AUTHORIZATION, "Bearer social-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].text").value("Hello"));
        authenticate(10L);
        mvc.perform(get("/publications/{id}", id).header(AUTHORIZATION, "Bearer social-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.likedByMe").value(false))
                .andExpect(jsonPath("$.images[0].storageKey").value(KEY));
        mvc.perform(get("/publications/{id}", eventId).header(AUTHORIZATION, "Bearer social-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.type").value("EVENT"))
                .andExpect(jsonPath("$.details.status").exists());
    }

    @Test @Transactional void draftIsHiddenAndCommentValidationUsesProblemDetails() throws Exception {
        var draft = (Notice) publications.save(Notice.create(7L, "Private", "Body", List.of(new PublicationImage(KEY, 0, true)), NOW));
        long id = draft.getId();
        when(membership.evaluate(anyLong())).thenReturn(MembershipAccessResult.ALLOWED);
        authenticate(9L);
        mvc.perform(post("/publications/{id}/likes", id).header(AUTHORIZATION, "Bearer social-token"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PUBLICATION_NOT_FOUND"));
        mvc.perform(get("/publications/{id}", id).header(AUTHORIZATION, "Bearer social-token"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PUBLICATION_NOT_FOUND"));
        draft.publish(NOW.plusSeconds(1));
        publications.save(draft);
        mvc.perform(post("/publications/{id}/comments", id).header(AUTHORIZATION, "Bearer social-token")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"  \",\"images\":[]}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PUBLICATION_COMMENT_INVALID"));
        doThrow(new StorageObjectNotFoundException()).when(media).requireExisting(KEY);
        mvc.perform(post("/publications/{id}/comments", id).header(AUTHORIZATION, "Bearer social-token")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"images\":[{\"storageKey\":\"" + KEY + "\",\"position\":0}]}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PUBLICATION_COMMENT_IMAGE_NOT_FOUND"));
    }

    private void authenticate(long userId) {
        when(jwt.parseAndValidate("social-token")).thenReturn(
                new JwtAuthenticatedUser(userId, 199L, "Member", Instant.now().plusSeconds(3600)));
        when(authorities.findAuthorityCodesByUserId(userId)).thenReturn(List.of(
                "PUBLICATIONS_INTERACTION_LIKE", "PUBLICATIONS_COMMENT_CREATE", "PUBLICATIONS_COMMENT_READ",
                "PUBLICATIONS_FEED_READ", "PUBLICATIONS_PUBLICATION_READ"));
    }
}
