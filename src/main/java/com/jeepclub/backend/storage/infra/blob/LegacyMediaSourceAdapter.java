package com.jeepclub.backend.storage.infra.blob;

import com.jeepclub.backend.storage.core.repository.LegacyMediaSource;
import com.jeepclub.backend.shared.storage.*;
import org.springframework.stereotype.Component;

@Component
public class LegacyMediaSourceAdapter implements LegacyMediaSource {
    private final FileStorage storage;
    public LegacyMediaSourceAdapter(FileStorage storage) {this.storage=storage;}
    @Override public byte[] read(String key) {
        if(key!=null && key.startsWith("private-images/")) PrivateImageReference.require(key);else ImageReference.require(key);
        byte[] bytes=storage.load(key).content();
        if(bytes.length==0 || bytes.length>10*1024*1024) throw new IllegalArgumentException("Legacy image exceeds custody limit");
        return bytes;
    }
}
