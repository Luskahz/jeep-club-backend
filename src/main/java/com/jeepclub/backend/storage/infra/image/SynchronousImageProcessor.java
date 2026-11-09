package com.jeepclub.backend.storage.infra.image;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.IIOImage;
import javax.imageio.ImageWriteParam;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Semaphore;

/** Bounded, synchronous codec. No persistence or access policy is implied by this component. */
public final class SynchronousImageProcessor {
    public static final int MAX_INPUT = 10 * 1024 * 1024;
    private static final long MAX_PIXELS = 20_000_000L;
    private static final int MAX_SIDE = 10_000;
    private final Semaphore capacity = new Semaphore(2, true);
    public enum Category { PROFILE, DEPENDENT, OTHER }
    public record Crop(int x, int y, int width, int height) {}
    public record Preset(String code, int maxWidth, int maxHeight, Crop crop) {
        public Preset {
            if (code == null || !code.matches("[A-Z][A-Z0-9_]{0,63}") || code.equals("MASTER")
                    || maxWidth < 1 || maxWidth > 4096 || maxHeight < 1 || maxHeight > 4096)
                throw new IllegalArgumentException("Invalid preset");
        }
    }
    public record Image(String code, byte[] bytes, String mimeType, int width, int height, String sha256) {}
    public record Result(String receivedSha256, Image master, List<Image> variants) {}

    public Result process(byte[] input, Category category, List<Preset> presets) throws IOException {
        if (input == null || input.length == 0 || input.length > MAX_INPUT || category == null || presets == null || presets.size() > 16
                || presets.stream().map(Preset::code).distinct().count() != presets.size())
            throw new IllegalArgumentException("Invalid upload");
        if (!capacity.tryAcquire()) throw new IllegalStateException("Image processing capacity exceeded");
        try {
            BufferedImage source = decode(input);
            int limit = (category == Category.OTHER ? 5 : 2) * 1024 * 1024;
            BufferedImage masterImage = source;
            byte[] masterBytes = encode(masterImage);
            // Reject rather than silently degrade below the chosen quality. Callers may retry with another image.
            if (masterBytes.length > limit) throw new IllegalArgumentException("MASTER exceeds category limit");
            String mime = masterImage.getColorModel().hasAlpha() ? "image/png" : "image/jpeg";
            Image master = image("MASTER", masterImage, masterBytes, mime);
            var variants = new java.util.ArrayList<Image>();
            for (Preset preset : presets) {
                BufferedImage cropped = crop(masterImage, preset.crop());
                BufferedImage resized = resize(cropped, preset.maxWidth(), preset.maxHeight());
                byte[] bytes = encode(resized);
                variants.add(image(preset.code(), resized, bytes, resized.getColorModel().hasAlpha() ? "image/png" : "image/jpeg"));
            }
            return new Result(hash(input), master, List.copyOf(variants));
        } finally { capacity.release(); }
    }
    private static BufferedImage decode(byte[] bytes) throws IOException {
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (stream == null) throw new IllegalArgumentException("Invalid image");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new IllegalArgumentException("Unsupported image");
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName();
                if (!format.equalsIgnoreCase("JPEG") && !format.equalsIgnoreCase("PNG")) throw new IllegalArgumentException("Unsupported image format");
                reader.setInput(stream, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > MAX_SIDE || height > MAX_SIDE || (long) width * height > MAX_PIXELS)
                    throw new IllegalArgumentException("Image dimensions exceed limits");
                BufferedImage image = reader.read(0);
                if (image == null) throw new IllegalArgumentException("Invalid image");
                return image;
            } finally { reader.dispose(); }
        }
    }
    private static BufferedImage crop(BufferedImage src, Crop c) {
        if (c == null) return src;
        if (c.x() < 0 || c.y() < 0 || c.width() < 1 || c.height() < 1 || (long)c.x()+c.width() > src.getWidth() || (long)c.y()+c.height() > src.getHeight())
            throw new IllegalArgumentException("Crop outside image");
        return src.getSubimage(c.x(), c.y(), c.width(), c.height());
    }
    private static BufferedImage resize(BufferedImage src, int maxW, int maxH) {
        double scale = Math.min(1d, Math.min((double) maxW/src.getWidth(), (double) maxH/src.getHeight()));
        int w = Math.max(1, (int)Math.round(src.getWidth()*scale)), h = Math.max(1, (int)Math.round(src.getHeight()*scale));
        BufferedImage out = new BufferedImage(w, h, src.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        try { g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC); g.drawImage(src, 0, 0, w, h, null); }
        finally { g.dispose(); }
        return out;
    }
    private static byte[] encode(BufferedImage image) throws IOException {
        boolean alpha = image.getColorModel().hasAlpha();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(alpha ? "png" : "jpeg");
        if (!writers.hasNext()) throw new IOException("Image writer unavailable");
        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream(); ImageOutputStream output = ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(output);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (!alpha) { param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT); param.setCompressionQuality(0.85f); }
            // Supplying no metadata deliberately strips EXIF/GPS from every persisted representation.
            writer.write(null, new IIOImage(image, null, null), param); output.flush();
            return bytes.toByteArray();
        } finally { writer.dispose(); }
    }
    private static Image image(String code, BufferedImage value, byte[] bytes, String mime) {
        return new Image(code, bytes, mime, value.getWidth(), value.getHeight(), hash(bytes));
    }
    private static String hash(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
