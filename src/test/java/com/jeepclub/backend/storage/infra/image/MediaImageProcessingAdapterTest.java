package com.jeepclub.backend.storage.infra.image;

import com.jeepclub.backend.storage.core.domain.enums.MediaContext;
import com.jeepclub.backend.storage.core.repository.MediaImageProcessor;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class MediaImageProcessingAdapterTest {
    @Test void cardIsOptInAndUsesIndependentPortraitCrop() throws Exception {
        var adapter=new MediaImageProcessingAdapter(new SimpleMeterRegistry());
        var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(120,80,BufferedImage.TYPE_INT_RGB),"png",out);
        byte[] bytes=out.toByteArray();
        var standard=adapter.process(bytes,MediaContext.IDENTITY_PROFILE);
        assertThat(standard.images()).extracting(MediaImageProcessor.Representation::code)
                .containsExactly("MASTER","AVATAR_SMALL","PROFILE_PREVIEW");
        var options=new MediaImageProcessor.Options(Map.of(
                "AVATAR_SMALL",new MediaImageProcessor.Crop(0,0,2d/3d,1),
                "CARD_PORTRAIT",new MediaImageProcessor.Crop(0,0,1d/3d,1)));
        var result=adapter.process(bytes,MediaContext.IDENTITY_PROFILE,options);
        assertThat(result.images()).extracting(MediaImageProcessor.Representation::code)
                .containsExactly("MASTER","AVATAR_SMALL","PROFILE_PREVIEW","CARD_PORTRAIT");
        assertThat(result.images().get(0).bytes()).containsExactly(standard.images().get(0).bytes());
        assertThat(result.images().get(1).width()).isEqualTo(result.images().get(1).height());
        assertThat(result.images().get(3).width()).isEqualTo(40);
        assertThat(result.images().get(3).height()).isEqualTo(80);
        assertThatThrownBy(() -> adapter.process(bytes,MediaContext.DEPENDENT_PHOTO,options)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.process(bytes,MediaContext.IDENTITY_PROFILE,
                new MediaImageProcessor.Options(Map.of("CARD_PORTRAIT",new MediaImageProcessor.Crop(0,0,1,1)))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.process(bytes,MediaContext.IDENTITY_PROFILE,
                new MediaImageProcessor.Options(Map.of("AVATAR_SMALL",new MediaImageProcessor.Crop(0,0,1,1)))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
