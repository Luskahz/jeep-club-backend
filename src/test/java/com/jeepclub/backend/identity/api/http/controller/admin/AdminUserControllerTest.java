package com.jeepclub.backend.identity.api.http.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jeepclub.backend.iam.authentication.core.application.exceptions.account.AuthenticationAccountNotFoundException;
import com.jeepclub.backend.iam.identity.api.http.controller.admin.AdminUserController;
import com.jeepclub.backend.iam.identity.api.http.exception.IdentityUserExceptionHandler;
import com.jeepclub.backend.iam.identity.api.module.UserStatus;
import com.jeepclub.backend.iam.identity.api.module.exception.UserAlreadyDisabledException;
import com.jeepclub.backend.iam.identity.api.module.exception.UserNotDisabledException;
import com.jeepclub.backend.iam.identity.api.module.exception.UserNotFoundException;
import com.jeepclub.backend.iam.identity.core.application.query.user.AdminUserField;
import com.jeepclub.backend.iam.identity.core.application.query.user.AdminUserFilter;
import com.jeepclub.backend.iam.identity.core.application.result.admin.user.AdminUserResult;
import com.jeepclub.backend.iam.identity.core.application.service.user.AdminUserService;
import com.jeepclub.backend.iam.identity.infra.exception.user.InvalidUserSortFieldException;
import com.jeepclub.backend.platform.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private AdminUserService adminUserService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new AdminUserController(adminUserService))
                .setControllerAdvice(new IdentityUserExceptionHandler(), new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setValidator(validator)
                .build();
    }

    @Test
    void listBindsPaginationFilterSearchFieldsAndSort() throws Exception {
        when(adminUserService.findAll(any(), any(), any())).thenReturn(new PageImpl<>(
                List.of(user(7L, UserStatus.ACTIVE)),
                PageRequest.of(1, 5, Sort.by(Sort.Direction.DESC, "name")),
                8
        ));

        mockMvc.perform(get("/identity/admin/users")
                        .param("id", "7")
                        .param("name", " Lucas ")
                        .param("q", " clube ")
                        .param("fields", "ID,NAME")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "name,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(7))
                .andExpect(jsonPath("$.content[0].name").value("Lucas Alves"));

        ArgumentCaptor<AdminUserFilter> filterCaptor = ArgumentCaptor.forClass(AdminUserFilter.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<AdminUserField>> fieldsCaptor = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        org.mockito.Mockito.verify(adminUserService).findAll(
                filterCaptor.capture(), fieldsCaptor.capture(), pageableCaptor.capture()
        );

        assertThat(filterCaptor.getValue().id()).isEqualTo(7L);
        assertThat(filterCaptor.getValue().name()).isEqualTo("Lucas");
        assertThat(filterCaptor.getValue().query()).isEqualTo("clube");
        assertThat(fieldsCaptor.getValue()).containsExactlyInAnyOrder(AdminUserField.ID, AdminUserField.NAME);
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("name").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void listUsesAllFieldsWhenFieldsParameterIsAbsent() throws Exception {
        when(adminUserService.findAll(any(), any(), any())).thenReturn(new PageImpl<>(
                List.of(), PageRequest.of(0, 20, Sort.by("id")), 0
        ));

        mockMvc.perform(get("/identity/admin/users"))
                .andExpect(status().isOk());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<AdminUserField>> fieldsCaptor = ArgumentCaptor.forClass(Set.class);
        org.mockito.Mockito.verify(adminUserService).findAll(any(), fieldsCaptor.capture(), any());
        assertThat(fieldsCaptor.getValue()).containsExactlyInAnyOrderElementsOf(
                EnumSet.allOf(AdminUserField.class)
        );
    }

    @Test
    void invalidSortReturnsRfcProblem() throws Exception {
        when(adminUserService.findAll(any(), any(), any()))
                .thenThrow(new InvalidUserSortFieldException("credentialStatus"));

        mockMvc.perform(get("/identity/admin/users").param("sort", "credentialStatus"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "cpf=529%20982%20247%2025",
            "id=0",
            "createdFrom=2026-02-01T00:00:00Z&createdTo=2026-01-01T00:00:00Z",
            "updatedFrom=2026-02-01T00:00:00Z&updatedTo=2026-01-01T00:00:00Z"
    })
    void invalidFiltersReturnValidationError(String query) throws Exception {
        mockMvc.perform(get("/identity/admin/users?" + query))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void findByIdReturnsUser() throws Exception {
        when(adminUserService.findById(7L)).thenReturn(user(7L, UserStatus.ACTIVE));

        mockMvc.perform(get("/identity/admin/users/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void findByIdReturnsNotFoundAndRejectsInvalidId() throws Exception {
        when(adminUserService.findById(99L)).thenThrow(new UserNotFoundException(99L));

        mockMvc.perform(get("/identity/admin/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_ID_NOT_FOUND"));
        mockMvc.perform(get("/identity/admin/users/0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void disableCoversSuccessNotFoundConflictAndMissingAccountIntegrity() throws Exception {
        when(adminUserService.disable(7L)).thenReturn(user(7L, UserStatus.DISABLED));
        when(adminUserService.disable(98L)).thenThrow(new UserNotFoundException(98L));
        when(adminUserService.disable(97L))
                .thenThrow(new UserAlreadyDisabledException(97L, new IllegalStateException()));
        when(adminUserService.disable(96L)).thenThrow(new AuthenticationAccountNotFoundException(96L));

        mockMvc.perform(patch("/identity/admin/users/7/disable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));
        mockMvc.perform(patch("/identity/admin/users/98/disable"))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/identity/admin/users/97/disable"))
                .andExpect(status().isConflict());
        mockMvc.perform(patch("/identity/admin/users/96/disable"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE"));
    }

    @Test
    void enableCoversSuccessNotFoundAndConflict() throws Exception {
        when(adminUserService.enable(7L)).thenReturn(user(7L, UserStatus.ACTIVE));
        when(adminUserService.enable(98L)).thenThrow(new UserNotFoundException(98L));
        when(adminUserService.enable(97L))
                .thenThrow(new UserNotDisabledException(97L, new IllegalStateException()));

        mockMvc.perform(patch("/identity/admin/users/7/enable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        mockMvc.perform(patch("/identity/admin/users/98/enable"))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/identity/admin/users/97/enable"))
                .andExpect(status().isConflict());
    }

    private AdminUserResult user(Long id, UserStatus status) {
        return new AdminUserResult(
                id, "Lucas Alves", LocalDate.of(2000, 5, 17), "lucas@example.com",
                "52998224725", "123456789", "5511999999999", null, status,
                CREATED_AT, status == UserStatus.DISABLED ? CREATED_AT.plusSeconds(60) : null,
                CREATED_AT.plusSeconds(60)
        );
    }
}
