package com.jeepclub.backend.memberships.core.domain.model;

import com.jeepclub.backend.memberships.core.domain.exception.MemberRegistryException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public record MembershipNumber(String value) {
    public static final java.math.BigInteger MAX_VALUE = java.math.BigInteger.TEN.pow(64).subtract(java.math.BigInteger.ONE);
    public MembershipNumber {
        if (value == null || value.length() > 64 || !value.matches("[0-9]+")) {
            throw new MemberRegistryException(MemberRegistryException.Kind.INVALID);
        }
        value = value.replaceFirst("^0+(?!$)", "");
        if (value.equals("0")) throw new MemberRegistryException(MemberRegistryException.Kind.INVALID);
    }

    /** Current occupancy/reservation key; released history is kept in audit, not here. */
    public String claimKey() {
        String key = "numeric:" + value;
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
