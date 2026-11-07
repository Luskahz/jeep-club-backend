package com.jeepclub.backend.identity.api.http;

import com.jeepclub.backend.iam.authentication.core.application.service.security.AccessTokenAuthenticationService;
import com.jeepclub.backend.iam.identity.api.module.UserStatus;
import com.jeepclub.backend.iam.identity.infra.persistence.entity.UserEntity;
import com.jeepclub.backend.iam.identity.infra.persistence.jpa.UserJpaRepository;
import com.jeepclub.backend.memberships.api.module.MembershipAccessQuery;
import com.jeepclub.backend.memberships.api.module.MembershipAccessResult;
import com.jeepclub.backend.platform.security.authorization.UserAuthoritiesProvider;
import com.jeepclub.backend.platform.security.jwt.JwtAuthenticatedUser;
import com.jeepclub.backend.platform.security.jwt.JwtTokenParser;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BirthdayHttpIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserJpaRepository users;
    @MockitoBean JwtTokenParser jwt;
    @MockitoBean UserAuthoritiesProvider authorities;
    @MockitoBean AccessTokenAuthenticationService authentication;
    @MockitoBean MembershipAccessQuery membership;

    @Test @Transactional void filtersAndOrdersMinimalProjectionAndAppliesMembership() throws Exception {
        save("Zoe", "00000000001", LocalDate.of(2000, 2, 29), UserStatus.ACTIVE);
        save("Ana", "00000000002", LocalDate.of(2004, 2, 1), UserStatus.ACTIVE);
        save("Inactive", "00000000003", LocalDate.of(2000, 2, 1), UserStatus.INACTIVE);
        save("Archived", "00000000005", LocalDate.of(2000, 2, 1), UserStatus.ARCHIVED);
        save("No date", "00000000004", null, UserStatus.ACTIVE);
        when(jwt.parseAndValidate("birthday-token")).thenReturn(new JwtAuthenticatedUser(99L, 199L, "Member", Instant.now().plusSeconds(3600)));
        when(authorities.findAuthorityCodesByUserId(99L)).thenReturn(List.of());
        when(membership.evaluate(anyLong())).thenReturn(MembershipAccessResult.ALLOWED);
        mvc.perform(get("/identity/users/birthdays").param("month", "2").header("Authorization", "Bearer birthday-token"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.month").value(2)).andExpect(jsonPath("$.birthdays.length()").value(2))
                .andExpect(jsonPath("$.birthdays[0].name").value("Ana")).andExpect(jsonPath("$.birthdays[1].birthDay").value(29))
                .andExpect(jsonPath("$.birthdays[0].birthMonth").value(2)).andExpect(jsonPath("$.birthdays[0].birthDate").doesNotExist())
                .andExpect(jsonPath("$.birthdays[0].cpf").doesNotExist());
        for (String month : List.of("0", "13", "abc"))
            mvc.perform(get("/identity/users/birthdays").param("month", month).header("Authorization", "Bearer birthday-token")).andExpect(status().isBadRequest());
        mvc.perform(get("/identity/users/birthdays").header("Authorization", "Bearer birthday-token")).andExpect(status().isBadRequest());
        mvc.perform(get("/identity/users/birthdays").param("month", "2")).andExpect(status().isUnauthorized());
        mvc.perform(get("/identity/users/birthdays").param("month", "3").header("Authorization", "Bearer birthday-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.birthdays.length()").value(0));
        when(membership.evaluate(anyLong())).thenReturn(MembershipAccessResult.CHARGE_NOT_FOUND);
        mvc.perform(get("/identity/users/birthdays").param("month", "2").header("Authorization", "Bearer birthday-token")).andExpect(status().isServiceUnavailable());
        when(membership.evaluate(anyLong())).thenReturn(MembershipAccessResult.PAYMENT_REQUIRED);
        mvc.perform(get("/identity/users/birthdays").param("month", "2").header("Authorization", "Bearer birthday-token")).andExpect(status().isPaymentRequired());
    }
    private void save(String name, String cpf, LocalDate birthDate, UserStatus status) {
        var user = new UserEntity(); user.setName(name); user.setCpf(cpf); user.setBirthDate(birthDate); user.setStatus(status); user.setCreatedAt(Instant.now()); users.saveAndFlush(user);
    }
}
