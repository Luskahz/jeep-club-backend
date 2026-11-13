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
    public record Image(String code, byte[] bytes, String mimeType, int width, int height, String sha256) {
        public Image { bytes = bytes.clone(); }
        @Override public byte[] bytes() { return bytes.clone(); }
    }
    public record Result(String receivedSha256, Image master, List<Image> variants) {}

    public Result process(byte[] input, Category category, List<Preset> presets) throws IOException {
        return process(input, category, presets, java.util.Map.of());
    }
    public Result process(byte[] input, Category category, List<Preset> presets,
                          java.util.Map<String, com.jeepclub.backend.storage.core.repository.MediaImageProcessor.Crop> crops) throws IOException {
        if (input == null || input.length == 0 || input.length > MAX_INPUT || category == null || presets == null || presets.size() > 16
                || crops == null || presets.stream().map(Preset::code).distinct().count() != presets.size()
                || !presets.stream().map(Preset::code).collect(java.util.stream.Collectors.toSet()).containsAll(crops.keySet()))
            throw new IllegalArgumentException("Invalid upload");
        if (!capacity.tryAcquire()) throw new IllegalStateException("Image processing capacity exceeded");
        try {
            byte[] snapshot = input.clone();
            BufferedImage decoded = decode(snapshot);
            int orientation = ExifOrientation.read(snapshot);
            BufferedImage source = ExifOrientation.apply(decoded, orientation);
            int limit = (category == Category.OTHER ? 5 : 2) * 1024 * 1024;
            byte[] preserved = orientation == 1 && source.getWidth() <= 4096 && source.getHeight() <= 4096
                    ? sanitizePngWithoutRecompression(snapshot) : null;
            if (preserved != null && preserved.length > limit) preserved = null;
            BufferedImage masterImage = resize(source, 4096, 4096);
            byte[] masterBytes = preserved != null ? preserved : encode(masterImage);
            while(masterBytes.length > limit && Math.max(masterImage.getWidth(),masterImage.getHeight()) > 256) {
                masterImage=resize(masterImage,Math.max(1,masterImage.getWidth()*3/4),Math.max(1,masterImage.getHeight()*3/4));
                masterBytes=encode(masterImage);
            }
            if (masterBytes.length > limit) throw new IllegalArgumentException("MASTER exceeds category limit");
            String mime = preserved != null || masterImage.getColorModel().hasAlpha() ? "image/png" : "image/jpeg";
            Image master = image("MASTER", masterImage, masterBytes, mime);
            // Derivatives use the actual persisted, sanitized MASTER, not a parallel source image.
            masterImage = decode(masterBytes);
            var variants = new java.util.ArrayList<Image>();
            long derivedPixels=0,totalBytes=masterBytes.length;
            for (Preset preset : presets) {
                Crop selected=preset.crop();
                var normalized = crops.get(preset.code());
                if (normalized != null) {
                    int x=(int)Math.floor(normalized.x()*masterImage.getWidth());
                    int y=(int)Math.floor(normalized.y()*masterImage.getHeight());
                    int right=Math.min(masterImage.getWidth(),(int)Math.ceil((normalized.x()+normalized.width())*masterImage.getWidth()));
                    int bottom=Math.min(masterImage.getHeight(),(int)Math.ceil((normalized.y()+normalized.height())*masterImage.getHeight()));
                    selected=new Crop(x,y,right-x,bottom-y);
                }
                BufferedImage cropped = crop(masterImage, selected);
                BufferedImage resized = resize(cropped, preset.maxWidth(), preset.maxHeight());
                derivedPixels+=(long)resized.getWidth()*resized.getHeight();
                if(derivedPixels>20_000_000L) throw new IllegalArgumentException("Variant pixel budget exceeded");
                var presentation=encodePresentation(resized);
                totalBytes+=presentation.bytes().length;
                if(totalBytes>20L*1024*1024) throw new IllegalArgumentException("Variant byte budget exceeded");
                variants.add(image(preset.code(), resized, presentation.bytes(), presentation.mime()));
            }
            return new Result(hash(snapshot), master, List.copyOf(variants));
        } finally { capacity.release(); }
    }
    /** Called only after full container and image validation. Unknown critical/color profiles use re-encoding. */
    private static byte[] sanitizePngWithoutRecompression(byte[] bytes) throws IOException {
        if (bytes.length < 8 || (bytes[0] & 255) != 137) return null;
        var kept = java.util.Set.of("IHDR", "PLTE", "IDAT", "IEND", "tRNS", "gAMA", "cHRM", "sRGB");
        var out = new ByteArrayOutputStream(bytes.length);
        out.write(bytes, 0, 8);
        int pos = 8;
        while (pos < bytes.length) {
            int n = java.nio.ByteBuffer.wrap(bytes, pos, 4).getInt();
            String type = new String(bytes, pos + 4, 4, java.nio.charset.StandardCharsets.US_ASCII);
            if (type.equals("iCCP") || (!kept.contains(type) && Character.isUpperCase(type.charAt(0)))) return null;
            if ((type.equals("gAMA") && n != 4) || (type.equals("cHRM") && n != 32)
                    || (type.equals("sRGB") && n != 1) || (type.equals("PLTE") && (n == 0 || n > 768 || n % 3 != 0))
                    || (type.equals("tRNS") && n > 256)) return null;
            if (kept.contains(type)) out.write(bytes, pos, n + 12);
            pos += n + 12;
        }
        return out.toByteArray();
    }
    private static BufferedImage decode(byte[] bytes) throws IOException {
        validateContainer(bytes);
        if (isWebp(bytes)) {
            if (!dev.matrixlab.webp4j.WebPCodec.isAvailable()) throw new IllegalStateException("WebP decoder unavailable");
            int[] dimensions=dev.matrixlab.webp4j.WebPCodec.getWebPInfo(bytes);
            if (dimensions==null || dimensions.length<2) throw new IllegalArgumentException("Invalid WebP dimensions");
            validateDimensions(dimensions[0],dimensions[1]);
            BufferedImage image=dev.matrixlab.webp4j.WebPCodec.decodeImage(bytes);
            if (image==null || image.getWidth()!=dimensions[0] || image.getHeight()!=dimensions[1])
                throw new IllegalArgumentException("Invalid WebP image");
            return image;
        }
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (stream == null) throw new IllegalArgumentException("Invalid image");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new IllegalArgumentException("Unsupported image");
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName();
                if (!format.equalsIgnoreCase("JPEG") && !format.equalsIgnoreCase("PNG")) throw new IllegalArgumentException("Unsupported image format");
                reader.setInput(stream, true, true);
                boolean[] warning = {false};
                reader.addIIOReadWarningListener((ignored, message) -> warning[0] = true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                validateDimensions(width,height);
                BufferedImage image = reader.read(0);
                if (image == null || warning[0]) throw new IllegalArgumentException("Invalid image");
                return image;
            } finally { reader.dispose(); }
        }
    }
    private static void validateDimensions(int width,int height) {
        if (width < 1 || height < 1 || width > MAX_SIDE || height > MAX_SIDE || (long) width * height > MAX_PIXELS)
            throw new IllegalArgumentException("Image dimensions exceed limits");
    }
    static boolean isWebp(byte[] b) {
        return b.length>=12 && b[0]=='R' && b[1]=='I' && b[2]=='F' && b[3]=='F'
                && b[8]=='W' && b[9]=='E' && b[10]=='B' && b[11]=='P';
    }
    /** Reject incomplete or corrupted containers even when a decoder tolerates them. */
    private static void validateContainer(byte[] bytes) {
        if (isWebp(bytes)) {
            long declared=Integer.toUnsignedLong(java.nio.ByteBuffer.wrap(bytes,4,4).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt());
            if (declared+8!=bytes.length) throw new IllegalArgumentException("Invalid WebP length");
            int pos=12;
            while (pos<=bytes.length-8) {
                long length=Integer.toUnsignedLong(java.nio.ByteBuffer.wrap(bytes,pos+4,4).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt());
                long end=pos+8L+length+(length&1);
                if (end>bytes.length) throw new IllegalArgumentException("Incomplete WebP chunk");
                String type=new String(bytes,pos,4,java.nio.charset.StandardCharsets.US_ASCII);
                if (type.equals("ANIM") || type.equals("ANMF") || (type.equals("VP8X") && length>=1 && (bytes[pos+8]&2)!=0))
                    throw new IllegalArgumentException("Animated WebP unsupported");
                pos=(int)end;
            }
            if (pos!=bytes.length) throw new IllegalArgumentException("Incomplete WebP");
            return;
        }
        if (bytes.length >= 4 && (bytes[0] & 255) == 0xff && (bytes[1] & 255) == 0xd8) {
            if ((bytes[bytes.length - 2] & 255) != 0xff || (bytes[bytes.length - 1] & 255) != 0xd9)
                throw new IllegalArgumentException("Incomplete JPEG");
            return;
        }
        byte[] signature = {(byte)137, 80, 78, 71, 13, 10, 26, 10};
        if (bytes.length < signature.length || !java.util.Arrays.equals(signature, java.util.Arrays.copyOf(bytes, signature.length)))
            throw new IllegalArgumentException("Unsupported image format");
        int pos = signature.length;
        boolean header = false, data = false;
        while (pos <= bytes.length - 12) {
            long length = Integer.toUnsignedLong(java.nio.ByteBuffer.wrap(bytes, pos, 4).getInt());
            if (length > bytes.length - (long) pos - 12) throw new IllegalArgumentException("Incomplete PNG");
            int n = (int) length;
            String type = new String(bytes, pos + 4, 4, java.nio.charset.StandardCharsets.US_ASCII);
            var crc = new java.util.zip.CRC32();
            crc.update(bytes, pos + 4, n + 4);
            long expected = Integer.toUnsignedLong(java.nio.ByteBuffer.wrap(bytes, pos + 8 + n, 4).getInt());
            if (crc.getValue() != expected) throw new IllegalArgumentException("Invalid PNG checksum");
            if (!header) {
                if (!type.equals("IHDR") || n != 13) throw new IllegalArgumentException("Invalid PNG header");
                header = true;
            } else if (type.equals("IHDR")) throw new IllegalArgumentException("Duplicate PNG header");
            if (type.equals("IDAT")) data = true;
            pos += n + 12;
            if (type.equals("IEND")) {
                if (n != 0 || !data || pos != bytes.length) throw new IllegalArgumentException("Invalid PNG end");
                return;
            }
        }
        throw new IllegalArgumentException("Incomplete PNG");
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
    record Encoded(byte[] bytes,String mime) {}
    @FunctionalInterface interface WebpEncoder { byte[] encode(BufferedImage image) throws IOException; }
    private static Encoded encodePresentation(BufferedImage image) throws IOException {
        return encodePresentation(image, i -> {
            if (!dev.matrixlab.webp4j.WebPCodec.isAvailable()) throw new IOException("WebP unavailable");
            return dev.matrixlab.webp4j.WebPCodec.encodeImage(i, 85f, i.getColorModel().hasAlpha());
        });
    }
    static Encoded encodePresentation(BufferedImage image, WebpEncoder encoder) throws IOException {
        try {
            byte[] bytes = encoder.encode(image);
            if (bytes == null || bytes.length < 12 || bytes[0]!='R' || bytes[1]!='I' || bytes[2]!='F' || bytes[3]!='F'
                    || bytes[8]!='W' || bytes[9]!='E' || bytes[10]!='B' || bytes[11]!='P') throw new IOException("Invalid WebP result");
            return new Encoded(bytes,"image/webp");
        } catch (IOException | RuntimeException | LinkageError unavailable) {
            return new Encoded(encode(image),image.getColorModel().hasAlpha()?"image/png":"image/jpeg");
        }
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
