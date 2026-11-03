package com.jeepclub.backend.publications.api.http;

import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.platform.web.exception.GlobalExceptionHandler;
import com.jeepclub.backend.publications.api.http.controller.admin.AdminEventController;
import com.jeepclub.backend.publications.api.http.controller.member.PublicationSocialController;
import com.jeepclub.backend.publications.api.http.dto.EventRequestReader;
import com.jeepclub.backend.publications.api.http.exception.*;
import com.jeepclub.backend.publications.core.application.service.*;
import com.jeepclub.backend.publications.core.domain.model.*;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;

class PublicationHardeningHttpTest {
    AdminEventService events = mock(AdminEventService.class);
    PublicationCommentService comments = mock(PublicationCommentService.class);
    PublicationLikeService likes = mock(PublicationLikeService.class);
    MockMvc mvc;
    @BeforeEach void setup() {
        var validator = new LocalValidatorFactoryBean();validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new AdminEventController(events,mock(EventRequestReader.class),Clock.systemUTC()),
                new PublicationSocialController(likes,comments))
            .setControllerAdvice(new EventExceptionHandler(new GlobalExceptionHandler()),new PublicationSocialExceptionHandler(new GlobalExceptionHandler()),new GlobalExceptionHandler())
            .setValidator(validator)
            .setCustomArgumentResolvers(new org.springframework.data.web.PageableHandlerMethodArgumentResolver(),
                new org.springframework.web.method.support.HandlerMethodArgumentResolver() {
                    public boolean supportsParameter(org.springframework.core.MethodParameter p) {return p.hasParameterAnnotation(org.springframework.security.core.annotation.AuthenticationPrincipal.class);}
                    public Object resolveArgument(org.springframework.core.MethodParameter p,org.springframework.web.method.support.ModelAndViewContainer c,
                            org.springframework.web.context.request.NativeWebRequest r,org.springframework.web.bind.support.WebDataBinderFactory f) {
                        return new UserPrincipal(9L,1L,"Synthetic",Instant.now().plusSeconds(60));
                    }
                }).build();
    }
    @Test void unexpectedEventNpeAndSqlRemainSanitizedServerErrors() throws Exception {
        for (var failure : List.of(new NullPointerException("private-sql-secret"),new DataAccessResourceFailureException("private-sql-secret"),
                integrity("unknown_constraint"))) {
            doThrow(failure).when(events).findAll(any());
            mvc.perform(get("/admin/events")).andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(content().string(not(containsString("private-sql-secret"))));
        }
    }
    @Test void onlyRecognizedEventUniquenessAndLockConflictsReturn409() throws Exception {
        for (var failure : List.of(integrity("uk_event_registration"),integrity("uk_event_ride_selected"),new CannotAcquireLockException("private-sql-secret"))) {
            doThrow(failure).when(events).findAll(any());
            mvc.perform(get("/admin/events")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EVENT_CONFLICT"));
        }
    }
    @Test void commentIntegrityIsNeverLabeledAsLikeConflict() throws Exception {
        when(comments.create(anyLong(),anyLong(),any(),any())).thenThrow(integrity("uk_publication_comment_image_position"));
        mvc.perform(post("/publications/1/comments").contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"valid\"}"))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"));
        for(String name:List.of("uk_publication_like_member","PUBLIC.UK_PUBLICATION_LIKE_MEMBER_INDEX_9","`publication_likes`.`uk_publication_like_member`")) {
            doThrow(integrity(name)).when(likes).like(anyLong(),anyLong());
            mvc.perform(post("/publications/1/likes")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PUBLICATION_LIKE_CONFLICT"));
        }
    }
    @Test void oversizedTextAndImagesAreRejectedBeforeServiceWork() throws Exception {
        for (var body : List.of("{\"text\":\""+"x".repeat(10001)+"\"}","{\"images\":["+String.join(",",Collections.nCopies(11,"{\"storageKey\":\"invalid\",\"position\":0}"))+"]}")) {
            mvc.perform(post("/publications/1/comments").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        }
        verifyNoInteractions(comments,likes,events);
    }
    private DataIntegrityViolationException integrity(String constraint) {
        return new DataIntegrityViolationException("private-sql-secret",new ConstraintViolationException("private-sql-secret",new java.sql.SQLException("private-sql-secret","23505"),constraint));
    }
}
