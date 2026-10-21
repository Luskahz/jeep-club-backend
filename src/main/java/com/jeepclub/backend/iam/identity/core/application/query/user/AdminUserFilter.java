package com.jeepclub.backend.iam.identity.core.application.query.user;

import com.jeepclub.backend.iam.identity.api.module.UserStatus;

import java.time.Instant;
import java.time.LocalDate;

public record AdminUserFilter(
        Long id,
        String name,
        LocalDate birthDate,
        String email,
        String cpf,
        String rg,
        String phoneNumber,
        UserStatus status,
        Instant createdFrom,
        Instant createdTo,
        Instant updatedFrom,
        Instant updatedTo,
        String query,
        String membershipNumber,
        Long membershipIdentityId,
        Long queryMembershipIdentityId
) {
    public AdminUserFilter(Long id, String name, LocalDate birthDate, String email, String cpf, String rg,
            String phoneNumber, UserStatus status, Instant createdFrom, Instant createdTo,
            Instant updatedFrom, Instant updatedTo, String query) {
        this(id,name,birthDate,email,cpf,rg,phoneNumber,status,createdFrom,createdTo,updatedFrom,updatedTo,query,null,null,null);
    }
    public AdminUserFilter resolvedMembership(Long exactId, Long queryId) {
        return new AdminUserFilter(id,name,birthDate,email,cpf,rg,phoneNumber,status,createdFrom,createdTo,
                updatedFrom,updatedTo,query,membershipNumber,exactId,queryId);
    }
}
