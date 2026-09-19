package com.jeepclub.backend.vehicles.core.application;

import java.util.Objects;

/**
 * Representa a presença de um campo num request de edição parcial: ausente do
 * JSON, enviado explicitamente como null, ou enviado com um valor. Distinguir
 * esses três estados é o que permite preservar campos omitidos em vez de
 * tratá-los como limpeza implícita.
 */
public final class FieldUpdate<T> {

    private enum Presence { OMITTED, EXPLICIT_NULL, VALUE }

    private static final FieldUpdate<?> OMITTED = new FieldUpdate<>(Presence.OMITTED, null);
    private static final FieldUpdate<?> EXPLICIT_NULL = new FieldUpdate<>(Presence.EXPLICIT_NULL, null);

    private final Presence presence;
    private final T value;

    private FieldUpdate(Presence presence, T value) {
        this.presence = presence;
        this.value = value;
    }

    @SuppressWarnings("unchecked")
    public static <T> FieldUpdate<T> omitted() {
        return (FieldUpdate<T>) OMITTED;
    }

    @SuppressWarnings("unchecked")
    public static <T> FieldUpdate<T> explicitNull() {
        return (FieldUpdate<T>) EXPLICIT_NULL;
    }

    public static <T> FieldUpdate<T> of(T value) {
        return new FieldUpdate<>(Presence.VALUE, Objects.requireNonNull(value));
    }

    public boolean isOmitted() {
        return presence == Presence.OMITTED;
    }

    public boolean isExplicitNull() {
        return presence == Presence.EXPLICIT_NULL;
    }

    public boolean isPresentValue() {
        return presence == Presence.VALUE;
    }

    public T value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FieldUpdate<?> that)) {
            return false;
        }
        return presence == that.presence && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(presence, value);
    }

    @Override
    public String toString() {
        return switch (presence) {
            case OMITTED -> "FieldUpdate.omitted()";
            case EXPLICIT_NULL -> "FieldUpdate.explicitNull()";
            case VALUE -> "FieldUpdate.of(" + value + ")";
        };
    }
}
