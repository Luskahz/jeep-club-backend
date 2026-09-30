package com.jeepclub.backend.iam.identity.core.domain.model;

public record WorkProfile(String occupation, String workplace) {
    public WorkProfile {
        occupation = UserProfile.normalize(occupation, "occupation", 150);
        workplace = UserProfile.normalize(workplace, "workplace", 150);
    }

    public boolean isComplete() {
        return occupation != null && workplace != null;
    }
}
