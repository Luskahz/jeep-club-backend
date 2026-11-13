package com.jeepclub.backend.storage.infra.blob;

import com.jeepclub.backend.storage.core.repository.LegacyMediaSource;
import com.jeepclub.backend.shared.storage.*;
import org.springframework.stereotype.Component;

@Component
public class LegacyMediaSourceAdapter implements LegacyMediaSource {
    private final FileStorage storage;
    public LegacyMediaSourceAdapter(FileStorage storage) {this.storage=storage;}
    @Override public Metadata identify(byte[] bytes) {
        try(var input=javax.imageio.ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(bytes))) {
            var readers=javax.imageio.ImageIO.getImageReaders(input);if(!readers.hasNext()) return LegacyMediaSource.super.identify(bytes);
            var reader=readers.next();try {
                reader.setInput(input,true,true);String mime=switch(reader.getFormatName().toUpperCase(java.util.Locale.ROOT)) {
                    case "JPEG", "JPG" -> "image/jpeg";case "PNG" -> "image/png";case "WEBP" -> "image/webp";default -> null;
                };
                int width=reader.getWidth(0),height=reader.getHeight(0);
                if(mime==null || width<=0 || height<=0 || width>10000 || height>10000 || (long)width*height>20_000_000L) return LegacyMediaSource.super.identify(bytes);
                // A readable header alone does not prove that the encoded image is intact.
                var decoded=reader.read(0);
                if(decoded==null || decoded.getWidth()!=width || decoded.getHeight()!=height) return LegacyMediaSource.super.identify(bytes);
                return new Metadata(mime,width,height);
            } finally {reader.dispose();}
        } catch(java.io.IOException | IllegalArgumentException invalid) {return LegacyMediaSource.super.identify(bytes);}
    }
    @Override public byte[] read(String key) {
        if(key!=null && key.startsWith("private-images/")) PrivateImageReference.require(key);else ImageReference.require(key);
        byte[] bytes=storage.load(key).content();
        if(bytes.length==0 || bytes.length>10*1024*1024) throw new IllegalArgumentException("Legacy image exceeds custody limit");
        return bytes;
    }
}
