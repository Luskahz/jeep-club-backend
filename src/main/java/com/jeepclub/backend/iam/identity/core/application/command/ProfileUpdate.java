package com.jeepclub.backend.iam.identity.core.application.command;

import java.time.LocalDate;
import java.util.Objects;

/** Presence is independent of the value: omitted preserves, present null clears. */
public record ProfileUpdate(
        Field<String> name,
        Field<LocalDate> birthDate,
        Field<String> rg,
        Field<String> phoneNumber,
        Field<String> profilePhotoStorageKey
) {
    public ProfileUpdate {
        Objects.requireNonNull(name);
        Objects.requireNonNull(birthDate);
        Objects.requireNonNull(rg);
        Objects.requireNonNull(phoneNumber);
        Objects.requireNonNull(profilePhotoStorageKey);
    }

    public boolean isEmpty() {
        return !name.present() && !birthDate.present() && !rg.present()
                && !phoneNumber.present() && !profilePhotoStorageKey.present();
    }

    public record Field<T>(boolean present, T value) {
        public static <T> Field<T> omitted() { return new Field<>(false, null); }
        public static <T> Field<T> provided(T value) { return new Field<>(true, value); }
        public T orElse(T current) { return present ? value : current; }
    }
}
