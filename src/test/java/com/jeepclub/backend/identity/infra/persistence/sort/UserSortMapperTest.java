package com.jeepclub.backend.identity.infra.persistence.sort;

import com.jeepclub.backend.iam.identity.infra.exception.user.InvalidUserSortFieldException;
import com.jeepclub.backend.iam.identity.infra.persistence.sort.UserSortMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserSortMapperTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "id", "name", "birthDate", "email", "cpf", "rg", "phoneNumber",
            "profilePhotoStorageKey", "status", "createdAt", "disabledAt", "updatedAt"
    })
    void acceptsEveryPublishedSortField(String field) {
        Pageable mapped = UserSortMapper.map(PageRequest.of(
                2, 15, Sort.by(Sort.Direction.ASC, field)
        ));

        assertThat(mapped.getPageNumber()).isEqualTo(2);
        assertThat(mapped.getPageSize()).isEqualTo(15);
        assertThat(mapped.getSort().getOrderFor(field).isAscending()).isTrue();
    }

    @Test
    void preservesDescendingDirection() {
        Pageable mapped = UserSortMapper.map(PageRequest.of(
                0, 20, Sort.by(Sort.Direction.DESC, "updatedAt")
        ));

        assertThat(mapped.getSort().getOrderFor("updatedAt").isDescending()).isTrue();
    }

    @Test
    void rejectsInternalOrUnknownSortField() {
        Pageable pageable = PageRequest.of(
                0, 20, Sort.by(Sort.Direction.ASC, "credentialStatus")
        );

        assertThatThrownBy(() -> UserSortMapper.map(pageable))
                .isInstanceOf(InvalidUserSortFieldException.class)
                .hasMessageContaining("credentialStatus");
    }
}
