package com.jeepclub.backend.storage.infra.image;

import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class SynchronousImageProcessorTest {
    private byte[] image() throws Exception {
        var img = new BufferedImage(120, 80, BufferedImage.TYPE_INT_RGB);
        var out = new ByteArrayOutputStream(); ImageIO.write(img, "png", out); return out.toByteArray();
    }
    @Test void derivesIndependentCropsAndHashesPersistedBytes() throws Exception {
        var processor = new SynchronousImageProcessor();
        var result = processor.process(image(), SynchronousImageProcessor.Category.PROFILE, List.of(
                new SynchronousImageProcessor.Preset("AVATAR_SMALL", 32, 32, new SynchronousImageProcessor.Crop(0, 0, 80, 80)),
                new SynchronousImageProcessor.Preset("CARD", 60, 80, new SynchronousImageProcessor.Crop(10, 0, 60, 80))));
        assertThat(result.master().bytes().length).isLessThanOrEqualTo(2 * 1024 * 1024);
        assertThat(result.variants()).extracting(SynchronousImageProcessor.Image::width).containsExactly(32, 60);
        assertThat(result.variants()).extracting(SynchronousImageProcessor.Image::height).containsExactly(32, 80);
        assertThat(result.master().sha256()).hasSize(64);
        assertThat(result.receivedSha256()).isNotEqualTo(result.master().sha256());
    }
    @Test void returnedBytesCannotMutateTheHashedRepresentation() throws Exception {
        var result = new SynchronousImageProcessor().process(image(), SynchronousImageProcessor.Category.PROFILE, List.of());
        byte[] original = result.master().bytes();
        byte[] exposed = result.master().bytes();
        exposed[0] ^= 1;
        assertThat(result.master().bytes()).containsExactly(original);
    }
    @Test void rejectsInvalidInputDuplicatePresetsAndOutOfBoundsCrop() throws Exception {
        var processor = new SynchronousImageProcessor();
        assertThatThrownBy(() -> processor.process(new byte[11 * 1024 * 1024], SynchronousImageProcessor.Category.OTHER, List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> processor.process(new byte[]{1,2,3}, SynchronousImageProcessor.Category.OTHER, List.of())).isInstanceOf(IllegalArgumentException.class);
        var preset = new SynchronousImageProcessor.Preset("FEED", 32, 32, new SynchronousImageProcessor.Crop(100, 0, 30, 30));
        assertThatThrownBy(() -> processor.process(image(), SynchronousImageProcessor.Category.OTHER, List.of(preset))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> processor.process(image(), SynchronousImageProcessor.Category.OTHER, List.of(preset, preset))).isInstanceOf(IllegalArgumentException.class);
    }
}
