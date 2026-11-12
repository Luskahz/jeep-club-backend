package com.jeepclub.backend.storage.api.http;

import com.jeepclub.backend.storage.core.application.*;
import com.jeepclub.backend.storage.core.domain.enums.MediaContext;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import java.io.IOException;

@RestController @RequestMapping("/storage/media")
@ConditionalOnProperty(name="storage.media.enabled",havingValue="true")
public class StorageMediaController {
    private final MediaUploadService upload;private final MediaResolutionService resolution;private final MediaReferenceService references;
    public StorageMediaController(MediaUploadService upload,MediaResolutionService resolution,MediaReferenceService references) { this.upload=upload;this.resolution=resolution;this.references=references; }
    @PostMapping(value="/profile",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaUploadService.Uploaded> upload(@AuthenticationPrincipal UserPrincipal user,@RequestPart("file") MultipartFile file) throws IOException {
        if(user==null) throw new AccessDeniedException("Media unavailable");
        if(file.getSize()<=0 || file.getSize()>10*1024*1024) throw new IllegalArgumentException("Invalid upload size");
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(upload.upload(user.getUserId(),MediaContext.IDENTITY_PROFILE,user.getUserId().toString(),() -> {
            try(var input=file.getInputStream()) {return input.readNBytes(10*1024*1024+1);}
            catch(IOException failure) {throw new java.io.UncheckedIOException(failure);}
        }));
    }
    public record Association(MediaContext context,String consumerId,String role) {}
    @PostMapping("/{mediaId}/references") public long associate(@AuthenticationPrincipal UserPrincipal user,@PathVariable long mediaId,@RequestBody Association association) {
        if(user==null) throw new AccessDeniedException("Media unavailable");
        // Only the owning consumer can change its aggregate and reference atomically.
        throw new AccessDeniedException("Media unavailable");
    }
    @DeleteMapping("/{mediaId}/references/{referenceId}") public void dissociate(@AuthenticationPrincipal UserPrincipal user,@PathVariable long mediaId,@PathVariable long referenceId) {
        throw new AccessDeniedException("Media unavailable");
    }
    @GetMapping("/{mediaId}/references/{referenceId}/variants/{code}")
    public ResponseEntity<byte[]> read(@AuthenticationPrincipal UserPrincipal user,@PathVariable long mediaId,@PathVariable long referenceId,@PathVariable String code) {
        if(user==null) throw new AccessDeniedException("Media unavailable");
        var content=resolution.resolve(user.getUserId(),mediaId,referenceId,code);
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL,"private, no-store")
            .contentType(MediaType.parseMediaType(content.mimeType())).body(content.bytes());
    }
}
