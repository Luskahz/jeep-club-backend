package com.jeepclub.backend.iam.identity.infra.storage;

import com.jeepclub.backend.storage.api.module.StorageConsumerPolicy;
import com.jeepclub.backend.storage.core.domain.enums.MediaContext;
import com.jeepclub.backend.storage.core.domain.model.MediaReference;
import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import org.springframework.stereotype.Component;

/** Initial isolated surface: only the active subject may manage/read their own profile media. */
@Component
public class ProfileStoragePolicy implements StorageConsumerPolicy {
    private final UserQuery users;
    public ProfileStoragePolicy(UserQuery users) { this.users=users; }
    @Override public MediaContext context() { return MediaContext.IDENTITY_PROFILE; }
    @Override public boolean mayUpload(long actor,String resource) {
        return Long.toString(actor).equals(resource) && users.isAdministrativelyActive(actor);
    }
    @Override public boolean mayRead(long actor,MediaReference reference,boolean master) {
        return reference.context()==context() && mayUpload(actor,reference.consumerId());
    }
}
