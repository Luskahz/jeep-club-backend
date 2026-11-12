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
    @Override public UploadIdentity uploadIdentity(long actor,String resource) {
        if(!mayUpload(actor,resource)) throw new org.springframework.security.access.AccessDeniedException("Media unavailable");
        return new UploadIdentity(actor,com.jeepclub.backend.storage.core.domain.enums.MediaSubjectType.USER,actor);
    }
    @Override public boolean mayAssociate(long actor,com.jeepclub.backend.storage.core.domain.model.MediaAsset asset,String resource,String role) {
        return "PHOTO".equals(role) && mayUpload(actor,resource) && Long.valueOf(actor).equals(asset.ownerUserId());
    }
    @Override public boolean mayDissociate(long actor,MediaReference reference) {
        return reference.context()==context() && mayUpload(actor,reference.consumerId());
    }
}
