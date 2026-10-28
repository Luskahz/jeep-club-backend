package com.jeepclub.backend.identity.core.application.service.user;

import com.jeepclub.backend.iam.identity.api.module.UserStatus;
import com.jeepclub.backend.iam.identity.core.application.query.user.AdminUserField;
import com.jeepclub.backend.iam.identity.core.application.query.user.AdminUserFilter;
import com.jeepclub.backend.iam.identity.core.application.result.admin.user.AdminUserResult;
import com.jeepclub.backend.iam.identity.core.application.service.user.UserOptionsService;
import com.jeepclub.backend.iam.identity.core.repository.AdminUserQueryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class UserOptionsServiceTest {
    @Test
    void selectsOnlyIdAndNameOfActiveUsersWithStableOrdering() {
        var repository = mock(AdminUserQueryRepository.class);
        var filter = new AdminUserFilter(null, "Maria", null, null, null, null, null,
                UserStatus.ACTIVE, null, null, null, null, null);
        var pageable = PageRequest.of(1, 10, Sort.by("name").ascending().and(Sort.by("id")));
        var user = new AdminUserResult(7L, "Maria Silva", null, null, null, null, null, null, null, null, null, null);
        when(repository.findAll(filter, Set.of(AdminUserField.ID, AdminUserField.NAME), pageable))
                .thenReturn(new PageImpl<>(List.of(user), pageable, 11));
        var result = new UserOptionsService(repository).find("  Maria  ", 1, 10);
        assertThat(result.getContent()).singleElement().satisfies(option -> {
            assertThat(option.id()).isEqualTo(7L);
            assertThat(option.name()).isEqualTo("Maria Silva");
        });
        assertThat(result.getTotalElements()).isEqualTo(11);
        verify(repository).findAll(filter, Set.of(AdminUserField.ID, AdminUserField.NAME), pageable);
    }
    @Test
    void treatsBlankSearchAsNoNameFilter() {
        var repository = mock(AdminUserQueryRepository.class);
        when(repository.findAll(any(), any(), any())).thenReturn(org.springframework.data.domain.Page.empty());
        new UserOptionsService(repository).find("   ", 0, 20);
        verify(repository).findAll(argThat(filter -> filter.name() == null && filter.status() == UserStatus.ACTIVE),
                eq(Set.of(AdminUserField.ID, AdminUserField.NAME)), any());
    }
}
