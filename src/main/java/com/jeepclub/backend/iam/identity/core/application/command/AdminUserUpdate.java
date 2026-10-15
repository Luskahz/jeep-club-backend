package com.jeepclub.backend.iam.identity.core.application.command;

import java.util.Objects;
import com.jeepclub.backend.iam.identity.core.application.command.ProfileUpdate.Field;

/** Administrative contact changes compose the existing presence-aware profile command. */
public record AdminUserUpdate(ProfileUpdate profile, Field<String> cpf, Field<String> email) {
    public AdminUserUpdate {
        Objects.requireNonNull(profile);
        Objects.requireNonNull(cpf);
        Objects.requireNonNull(email);
    }
    public boolean isEmpty() { return profile.isEmpty() && !cpf.present() && !email.present(); }
}
