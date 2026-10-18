package com.jeepclub.backend.memberships.core.application.result;
public record MemberImportRow(Long identityUserId, String membershipNumber, boolean verified, String verificationReference) { }
