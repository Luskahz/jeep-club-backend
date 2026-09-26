package com.jeepclub.backend.iam.identity.infra.integration.media;

import com.jeepclub.backend.iam.identity.core.port.ProfileImagePort;
import com.jeepclub.backend.platform.storage.image.ImageMediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProfileImageAdapter implements ProfileImagePort {
    private final ImageMediaService images;

    @Override
    public void requireExisting(String storageKey) {
        images.requireExisting(storageKey);
    }
}
