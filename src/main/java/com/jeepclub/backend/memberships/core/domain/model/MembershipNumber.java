package com.jeepclub.backend.memberships.core.domain.model;

import com.jeepclub.backend.memberships.core.domain.exception.MemberRegistryException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public record MembershipNumber(String value) {
    public MembershipNumber {
        if (value == null || value.isBlank() || value.length() > 64 || !value.equals(value.strip())
                || value.codePoints().anyMatch(c -> Character.isISOControl(c) || Character.getType(c) == Character.FORMAT
                || Character.getType(c) == Character.SURROGATE)) {
            throw new MemberRegistryException(MemberRegistryException.Kind.INVALID);
        }
    }

    /** A conservative ambiguity guard, not a rewrite of the presentation value. */
    public String claimKey() {
        String key = value.matches("[0-9]+") ? "numeric:" + value.replaceFirst("^0+(?!$)", "") : "literal:" + value;
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
