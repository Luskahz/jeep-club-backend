package com.jeepclub.backend.storage.infra.image;

import com.jeepclub.backend.storage.core.repository.MediaImageProcessor;
import com.jeepclub.backend.storage.core.domain.enums.MediaContext;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

@Component
public class MediaImageProcessingAdapter implements MediaImageProcessor {
    private final SynchronousImageProcessor processor=new SynchronousImageProcessor();
    private final MeterRegistry metrics;
    public MediaImageProcessingAdapter(MeterRegistry metrics) { this.metrics=metrics; }
    @Override public Processed process(byte[] input, MediaContext context) {
        long start=System.nanoTime(); String outcome="failed";
        try {
            boolean profile=context==MediaContext.IDENTITY_PROFILE || context==MediaContext.DEPENDENT_PHOTO;
            var presets=profile ? List.of(new SynchronousImageProcessor.Preset("AVATAR_SMALL",64,64,null),
                new SynchronousImageProcessor.Preset("PROFILE_PREVIEW",320,320,null))
                : List.of(new SynchronousImageProcessor.Preset("THUMBNAIL",240,180,null),
                new SynchronousImageProcessor.Preset("GALLERY",1600,1200,null));
            var result=processor.process(input,profile ? SynchronousImageProcessor.Category.PROFILE : SynchronousImageProcessor.Category.OTHER,presets);
            var images=new ArrayList<Representation>(); images.add(convert(result.master())); result.variants().forEach(i -> images.add(convert(i)));
            outcome="success"; return new Processed(result.receivedSha256(),images);
        } catch(IOException e) { throw new IllegalArgumentException("Invalid image content",e); }
        finally { metrics.timer("storage.image.processing","outcome",outcome).record(System.nanoTime()-start,java.util.concurrent.TimeUnit.NANOSECONDS); }
    }
    private static Representation convert(SynchronousImageProcessor.Image image) {
        return new Representation(image.code(),image.bytes(),image.mimeType(),image.width(),image.height());
    }
}
