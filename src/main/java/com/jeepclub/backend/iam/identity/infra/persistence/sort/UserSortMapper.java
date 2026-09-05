package com.jeepclub.backend.iam.identity.infra.persistence.sort;

import com.jeepclub.backend.iam.identity.infra.exception.user.InvalidUserSortFieldException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class UserSortMapper {
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "name", "birthDate", "email", "cpf", "rg", "phoneNumber",
            "profilePhotoUrl", "status", "createdAt", "disabledAt", "updatedAt"
    );

    private UserSortMapper() {
    }

    public static Pageable map(Pageable pageable) {
        Sort mappedSort = Sort.by(pageable.getSort().stream()
                .map(UserSortMapper::validate)
                .toList());
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), mappedSort);
    }

    private static Sort.Order validate(Sort.Order order) {
        if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
            throw new InvalidUserSortFieldException(order.getProperty());
        }
        return order;
    }
}
