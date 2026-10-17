package com.jeepclub.backend.memberships.api.http.controller.admin;

import com.jeepclub.backend.memberships.api.http.exception.MembershipExceptionHandler;
import com.jeepclub.backend.memberships.core.application.exception.MembershipApplicantBlockNotFoundException;
import com.jeepclub.backend.memberships.core.application.service.membershipapplicantblock.AdminMembershipApplicantBlockService;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.platform.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminMembershipApplicantBlockControllerTest {

    private static final String CPF = "52998224725";
    private static final Long ADMIN_USER_ID = 10L;

    @Mock
    private AdminMembershipApplicantBlockService adminMembershipApplicantBlockService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        JsonMapper jsonMapper = JsonMapper.builder()
                .findAndAddModules()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();

        UserPrincipal principal = new UserPrincipal(ADMIN_USER_ID, 1L, "Admin", Instant.now());

        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AdminMembershipApplicantBlockController(adminMembershipApplicantBlockService)
                )
                .setControllerAdvice(new MembershipExceptionHandler(), new GlobalExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(jsonMapper))
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType().equals(UserPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return principal;
                    }
                })
                .build();
    }

    @Test
    @DisplayName("POST /admin/membership-applications/blocks/{cpf}/unblock should return 204 No Content on success")
    void shouldReturn204OnUnblockSuccess() throws Exception {
        doNothing().when(adminMembershipApplicantBlockService).unblock(CPF, ADMIN_USER_ID);

        mockMvc.perform(post("/admin/membership-applications/blocks/{cpf}/unblock", CPF))
                .andExpect(status().isNoContent());

        verify(adminMembershipApplicantBlockService).unblock(CPF, ADMIN_USER_ID);
    }

    @Test
    @DisplayName("POST /admin/membership-applications/blocks/{cpf}/unblock should return 404 Not Found RFC 9457 when block not found")
    void shouldReturn404WhenBlockNotFound() throws Exception {
        doThrow(new MembershipApplicantBlockNotFoundException(CPF))
                .when(adminMembershipApplicantBlockService).unblock(CPF, ADMIN_USER_ID);

        mockMvc.perform(post("/admin/membership-applications/blocks/{cpf}/unblock", CPF))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBERSHIP_APPLICANT_BLOCK_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404));
    }
}
