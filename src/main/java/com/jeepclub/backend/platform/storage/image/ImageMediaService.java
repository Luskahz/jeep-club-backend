package com.jeepclub.backend.platform.storage.image;

import com.jeepclub.backend.shared.storage.FileStorage;
import com.jeepclub.backend.shared.storage.ImageReference;
import com.jeepclub.backend.shared.storage.StorageFile;
import com.jeepclub.backend.shared.storage.StorageResource;
import com.jeepclub.backend.shared.storage.exception.InvalidStorageFileException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ImageMediaService {
    private static final int MAX_BYTES = 5 * 1024 * 1024;
    private final FileStorage storage;

    public String store(String filename, String contentType, byte[] bytes) {
        if (filename == null || contentType == null || bytes == null || bytes.length == 0 || bytes.length > MAX_BYTES) {
            throw new InvalidStorageFileException("Image filename, content type and non-empty content up to 5 MB are required.");
        }
        int dot = filename.lastIndexOf('.');
        String extension = dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (extension.equals("jpeg")) extension = "jpg";
        String expected = switch (extension) {
            case "jpg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> throw new InvalidStorageFileException("Only JPEG, PNG and WebP images are supported.");
        };
        if (!expected.equalsIgnoreCase(contentType) || !matchesSignature(extension, bytes)) {
            throw new InvalidStorageFileException("Image extension, content type and file signature must match.");
        }
        return storage.store(new StorageFile(filename, expected, extension, bytes), "images").storageKey();
    }

    public StorageResource load(String key) {
        return storage.load(ImageReference.require(key));
    }

    public String requireExisting(String key) {
        if (key != null) load(key);
        return key;
    }

    public static String contentType(String key) {
        ImageReference.require(key);
        if (key.endsWith(".png")) return "image/png";
        if (key.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    private static boolean matchesSignature(String extension, byte[] bytes) {
        return switch (extension) {
            case "jpg" -> bytes.length >= 3 && (bytes[0] & 255) == 0xff && (bytes[1] & 255) == 0xd8 && (bytes[2] & 255) == 0xff;
            case "png" -> bytes.length >= 8 && Arrays.equals(Arrays.copyOf(bytes, 8), new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10});
            case "webp" -> bytes.length >= 12 && new String(bytes, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")
                    && new String(bytes, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP");
            default -> false;
        };
    }
}
