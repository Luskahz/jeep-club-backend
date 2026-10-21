package com.jeepclub.backend.platform.storage.image;

import com.jeepclub.backend.shared.storage.exception.InvalidStorageFileException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import java.io.IOException;

/** HTTP adapter; application services receive only ImageUpload. */
public final class ImageMultipartAdapter {
    private ImageMultipartAdapter() {}

    public static void validateParts(MultipartHttpServletRequest request, String dataPart, String imagePart, int maxImages) {
        for (var entry : request.getMultiFileMap().entrySet()) {
            int limit = entry.getKey().equals(dataPart) ? 1 : entry.getKey().equals(imagePart) ? maxImages : 0;
            if (entry.getValue().size() > limit) {
                throw new InvalidStorageFileException("Unexpected multipart fields or number of image files.");
            }
        }
        if (request.getParameterMap().containsKey(imagePart)) {
            throw new InvalidStorageFileException("Images must be files, not textual references.");
        }
    }

    public static ImageUpload read(MultipartFile file) throws IOException {
        if (file == null) return null;
        if (file.isEmpty() || file.getSize() > ImageUpload.MAX_BYTES) {
            throw new InvalidStorageFileException("A non-empty image up to 5 MB is required.");
        }
        return new ImageUpload(file.getOriginalFilename(), file.getContentType(), file.getBytes());
    }
}
