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
        assertThat(result.variants()).extracting(SynchronousImageProcessor.Image::mimeType).containsExactly("image/webp", "image/webp");
        assertThat(result.master().sha256()).hasSize(64);
        assertThat(result.receivedSha256()).isEqualTo(result.master().sha256());
    }
    @Test void returnedBytesCannotMutateTheHashedRepresentation() throws Exception {
        var result = new SynchronousImageProcessor().process(image(), SynchronousImageProcessor.Category.PROFILE, List.of());
        byte[] original = result.master().bytes();
        byte[] exposed = result.master().bytes();
        exposed[0] ^= 1;
        assertThat(result.master().bytes()).containsExactly(original);
    }
    @Test void normalizedCropsAreIndependentAndOmissionDoesNotCrop() throws Exception {
        var processor = new SynchronousImageProcessor();
        var presets = List.of(new SynchronousImageProcessor.Preset("AVATAR_SMALL", 64, 64, null),
                new SynchronousImageProcessor.Preset("CARD", 64, 64, null));
        byte[] source = image();
        var uncropped = processor.process(source, SynchronousImageProcessor.Category.PROFILE, presets);
        var crops = java.util.Map.of("AVATAR_SMALL", new com.jeepclub.backend.storage.core.repository.MediaImageProcessor.Crop(0, 0, 2d/3d, 1),
                "CARD", new com.jeepclub.backend.storage.core.repository.MediaImageProcessor.Crop(0, 0, .5, 1));
        var cropped = processor.process(source, SynchronousImageProcessor.Category.PROFILE, presets, crops);
        assertThat(uncropped.variants().get(0).width()).isEqualTo(64);
        assertThat(uncropped.variants().get(0).height()).isEqualTo(43);
        assertThat(cropped.variants().get(0).width()).isEqualTo(64);
        assertThat(cropped.variants().get(0).height()).isEqualTo(64);
        assertThat(cropped.variants().get(1).width()).isEqualTo(48);
        assertThat(cropped.variants().get(1).height()).isEqualTo(64);
        assertThat(cropped.master().bytes()).containsExactly(uncropped.master().bytes());
        assertThatThrownBy(() -> new com.jeepclub.backend.storage.core.repository.MediaImageProcessor.Crop(Double.NaN, 0, 1, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new com.jeepclub.backend.storage.core.repository.MediaImageProcessor.Crop(.5, 0, .6, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> processor.process(source, SynchronousImageProcessor.Category.PROFILE, presets,
                java.util.Map.of("MASTER", crops.get("CARD")))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void rejectsInvalidInputDuplicatePresetsAndOutOfBoundsCrop() throws Exception {
        var processor = new SynchronousImageProcessor();
        assertThatThrownBy(() -> processor.process(new byte[11 * 1024 * 1024], SynchronousImageProcessor.Category.OTHER, List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> processor.process(new byte[]{1,2,3}, SynchronousImageProcessor.Category.OTHER, List.of())).isInstanceOf(IllegalArgumentException.class);
        var preset = new SynchronousImageProcessor.Preset("FEED", 32, 32, new SynchronousImageProcessor.Crop(100, 0, 30, 30));
        assertThatThrownBy(() -> processor.process(image(), SynchronousImageProcessor.Category.OTHER, List.of(preset))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> processor.process(image(), SynchronousImageProcessor.Category.OTHER, List.of(preset, preset))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void rejectsTruncatedJpegAndPngWithBrokenCrc() throws Exception {
        var processor = new SynchronousImageProcessor();
        var out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(12, 8, BufferedImage.TYPE_INT_RGB), "jpeg", out);
        byte[] jpeg = out.toByteArray();
        assertThatThrownBy(() -> processor.process(java.util.Arrays.copyOf(jpeg, jpeg.length - 2),
                SynchronousImageProcessor.Category.PROFILE, List.of())).isInstanceOf(IllegalArgumentException.class);
        byte[] png = image();
        png[29] ^= 1; // IHDR CRC, without altering the dimensions.
        assertThatThrownBy(() -> processor.process(png, SynchronousImageProcessor.Category.PROFILE, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        byte[] valid = image();
        assertThatThrownBy(() -> processor.process(java.util.Arrays.copyOf(valid, valid.length - 12),
                SynchronousImageProcessor.Category.PROFILE, List.of())).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void pngMasterDropsTextWithoutRecompressingPixelsOrLosingTransparency() throws Exception {
        var input = new BufferedImage(12, 8, BufferedImage.TYPE_INT_ARGB);
        input.setRGB(2, 3, 0x40112233);
        var encoded = new ByteArrayOutputStream(); ImageIO.write(input, "png", encoded);
        byte[] original = encoded.toByteArray();
        byte[] text = "Comment\0private location".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
        var chunk = new ByteArrayOutputStream();
        var data = new java.io.DataOutputStream(chunk);
        data.writeInt(text.length); data.writeBytes("tEXt"); data.write(text);
        var crc = new java.util.zip.CRC32(); crc.update("tEXt".getBytes(java.nio.charset.StandardCharsets.US_ASCII)); crc.update(text);
        data.writeInt((int) crc.getValue());
        var withMetadata = new ByteArrayOutputStream();
        withMetadata.write(original, 0, 33); withMetadata.write(chunk.toByteArray()); withMetadata.write(original, 33, original.length - 33);
        var result = new SynchronousImageProcessor().process(withMetadata.toByteArray(), SynchronousImageProcessor.Category.PROFILE, List.of());
        assertThat(result.master().mimeType()).isEqualTo("image/png");
        assertThat(result.master().bytes()).containsExactly(original);
        assertThat(result.receivedSha256()).isNotEqualTo(result.master().sha256());
        assertThat(ImageIO.read(new java.io.ByteArrayInputStream(result.master().bytes())).getRGB(2,3)).isEqualTo(0x40112233);
    }
    @Test void webpCodecPreservesTransparencyAndHasSafeFallbacks() throws Exception {
        assertThat(dev.matrixlab.webp4j.WebPCodec.isAvailable()).isTrue();
        var image = new BufferedImage(12, 8, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(2, 3, 0x40112233);
        var encoded = SynchronousImageProcessor.encodePresentation(image,
                value -> dev.matrixlab.webp4j.WebPCodec.encodeLosslessImage(value));
        assertThat(encoded.mime()).isEqualTo("image/webp");
        assertThat(dev.matrixlab.webp4j.WebPCodec.decodeImage(encoded.bytes()).getRGB(2,3)).isEqualTo(0x40112233);
        var pngFallback = SynchronousImageProcessor.encodePresentation(image, value -> { throw new java.io.IOException("codec failed"); });
        assertThat(pngFallback.mime()).isEqualTo("image/png");
        assertThat(ImageIO.read(new java.io.ByteArrayInputStream(pngFallback.bytes())).getRGB(2,3)).isEqualTo(0x40112233);
        var jpegFallback = SynchronousImageProcessor.encodePresentation(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB), value -> new byte[]{1,2});
        assertThat(jpegFallback.mime()).isEqualTo("image/jpeg");
        assertThat(ImageIO.read(new java.io.ByteArrayInputStream(jpegFallback.bytes()))).isNotNull();
    }
    @Test void webpMasterRemovesMetadataWithoutRecompressingImage() throws Exception {
        var image = new BufferedImage(12, 8, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(2,3,0x40112233);
        byte[] original=dev.matrixlab.webp4j.WebPCodec.encodeLosslessImage(image);
        var out=new ByteArrayOutputStream(); out.write(original);
        byte[] metadata="private-location".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        out.write(new byte[]{'X','M','P',' ',(byte)metadata.length,0,0,0}); out.write(metadata);
        if ((metadata.length&1)!=0) out.write(0);
        byte[] uploaded=out.toByteArray();
        java.nio.ByteBuffer.wrap(uploaded,4,4).order(java.nio.ByteOrder.LITTLE_ENDIAN).putInt(uploaded.length-8);
        var result=new SynchronousImageProcessor().process(uploaded,SynchronousImageProcessor.Category.PROFILE,List.of());
        assertThat(result.master().mimeType()).isEqualTo("image/webp");
        assertThat(result.master().bytes()).containsExactly(original);
        assertThat(result.receivedSha256()).isNotEqualTo(result.master().sha256());
        assertThat(dev.matrixlab.webp4j.WebPCodec.decodeImage(result.master().bytes()).getRGB(2,3)).isEqualTo(0x40112233);
    }
}
