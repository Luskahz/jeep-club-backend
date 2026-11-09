package com.jeepclub.backend.storage.core.repository;

import com.jeepclub.backend.storage.core.domain.enums.MediaContext;
import java.util.List;

public interface MediaImageProcessor {
    record Representation(String code, byte[] bytes, String mimeType, int width, int height) {
        public Representation { bytes=bytes.clone(); }
        @Override public byte[] bytes() { return bytes.clone(); }
    }
    record Processed(String receivedSha256, List<Representation> images) {
        public Processed { images=List.copyOf(images); }
    }
    Processed process(byte[] input, MediaContext context);
}
