package com.jeepclub.backend.authentication.infra.persistence.entity;

import jakarta.persistence.Column;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserEntityMappingTest {

    @Test
    void usesMigratedColumnNamesAndDomainCreatedAt() throws Exception {
        Column birthDate = UserEntity.class.getDeclaredField("birthDate")
                .getAnnotation(Column.class);
        Column passwordChangedAt = UserEntity.class.getDeclaredField("passwordChangedAt")
                .getAnnotation(Column.class);
        Column createdAt = UserEntity.class.getDeclaredField("createdAt")
                .getAnnotation(Column.class);

        assertThat(birthDate.name()).isEqualTo("birth_date");
        assertThat(passwordChangedAt.name()).isEqualTo("password_changed_at");
        assertThat(createdAt.updatable()).isFalse();
        assertThat(UserEntity.class.getDeclaredField("createdAt").getAnnotations())
                .noneMatch(annotation -> annotation.annotationType().getSimpleName()
                        .equals("CreationTimestamp"));
    }
}
