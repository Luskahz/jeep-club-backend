package com.jeepclub.backend.authentication.infra.persistence.sort;

import com.jeepclub.backend.authentication.infra.exception.user.InvalidUserSortFieldException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;

public final class UserSortMapper {

    private static final Map<String, String> ALLOWED_SORT_FIELDS = Map.of(
            "id", "id",
            "name", "name",
            "cpf", "cpf",
            "email", "email",
            "phoneNumber", "phoneNumber",
            "createdAt", "createdAt",
            "updatedAt", "updatedAt"
    );

    private UserSortMapper() {
    }

    public static Pageable map(Pageable pageable) {
        Sort mappedSort = Sort.by(
                pageable.getSort()
                        .stream()
                        .map(UserSortMapper::mapOrder)
                        .toList()
        );

        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                mappedSort
        );
    }

    private static Sort.Order mapOrder(Sort.Order order) {
        String mappedProperty =
                ALLOWED_SORT_FIELDS.get(order.getProperty());

        if (mappedProperty == null) {
            throw new InvalidUserSortFieldException(
                    order.getProperty()
            );
        }

        return order.withProperty(mappedProperty);
    }
}