package com.jeepclub.backend.platform.export;
import com.jeepclub.backend.shared.export.ExportFile;
import org.springframework.http.*;
public final class ExportDownload {
    private ExportDownload() {}
    public static ResponseEntity<byte[]> response(ExportFile file) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.contentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.filename()).build().toString())
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .header("X-Content-Type-Options", "nosniff")
            .contentLength(file.bytes().length).body(file.bytes());
    }
}
