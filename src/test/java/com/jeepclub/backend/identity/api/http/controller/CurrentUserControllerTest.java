package com.jeepclub.backend.identity.api.http.controller;

import com.jeepclub.backend.iam.identity.api.http.controller.CurrentUserController;
import com.jeepclub.backend.iam.identity.api.module.UserDetails;
import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import com.jeepclub.backend.iam.identity.api.module.UserStatus;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CurrentUserControllerTest {
    @Test
    void returnsOnlyIdentityOwnedUserFields() {
        UserQuery userQuery = mock(UserQuery.class);
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        when(userQuery.findById(42L)).thenReturn(Optional.of(new UserDetails(
                42L, "User", LocalDate.of(1990, 1, 1), "user@example.com",
                "52998224725", "123456789", "5511999999999", "photo.jpg",
                true, createdAt, null, null
        )));
        var controller = new CurrentUserController(userQuery);

        var body = controller.getMe(new UserPrincipal(42L, 7L, createdAt.plusSeconds(900)))
                .getBody();

        assertThat(body).isNotNull();
        assertThat(body.id()).isEqualTo(42L);
        assertThat(body.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(body.cpf()).isEqualTo("52998224725");
    }
}
