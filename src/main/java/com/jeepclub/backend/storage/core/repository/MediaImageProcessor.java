package com.jeepclub.backend.storage.core.repository;

import com.jeepclub.backend.storage.core.domain.enums.MediaContext;
import java.util.List;
import java.util.Map;

public interface MediaImageProcessor {
    record Representation(String code, byte[] bytes, String mimeType, int width, int height) {
        public Representation { bytes=bytes.clone(); }
        @Override public byte[] bytes() { return bytes.clone(); }
    }
    record Processed(String receivedSha256, List<Representation> images) {
        public Processed { images=List.copyOf(images); }
    }
    /** Normalized rectangle on the oriented MASTER, with origin at its top left. */
    record Crop(double x, double y, double width, double height) {
        public Crop {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(width) || !Double.isFinite(height)
                    || x < 0 || y < 0 || width <= 0 || height <= 0 || x + width > 1 || y + height > 1)
                throw new IllegalArgumentException("Invalid crop");
        }
    }
    record Options(Map<String, Crop> crops) {
        public Options { crops = crops == null ? Map.of() : Map.copyOf(crops); }
        public static Options defaults() { return new Options(Map.of()); }
    }
    Processed process(byte[] input, MediaContext context);
    default Processed process(byte[] input, MediaContext context, Options options) {
        if (options == null || !options.crops().isEmpty()) throw new IllegalArgumentException("Crop unsupported");
        return process(input, context);
    }
}
