package com.jeepclub.backend.dependents.api.http.dto.dependent;

import com.jeepclub.backend.platform.storage.image.ImageMediaContent;
import com.jeepclub.backend.platform.storage.image.ImageMultipartAdapter;
import com.jeepclub.backend.shared.storage.exception.InvalidStorageFileException;
import org.springframework.http.*;
import org.springframework.web.multipart.MultipartHttpServletRequest;

public final class DependentPhotoHttp {
    private DependentPhotoHttp() {}
    public static void validateParts(MultipartHttpServletRequest request,boolean structuredData) {
        ImageMultipartAdapter.validateParts(request,structuredData?"dependent":null,"photo",1);
        request.getParameterMap().forEach((name,values) -> {
            if (!structuredData || !name.equals("dependent") || values.length!=1) {
                throw new InvalidStorageFileException("Unexpected multipart fields; send a structured dependent part and image file only.");
            }
        });
    }
    public static ResponseEntity<byte[]> image(ImageMediaContent photo) {
        String extension=switch(photo.contentType()) { case "image/png" -> "png";case "image/webp" -> "webp";default -> "jpg"; };
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.parseMediaType(photo.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,"inline; filename=\"dependent-photo."+extension+"\"")
                .contentLength(photo.size()).body(photo.content());
    }
}
