package com.jeepclub.backend.storage.infra.image;

import org.junit.jupiter.api.Test;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import javax.imageio.ImageIO;
import static org.assertj.core.api.Assertions.*;

class ExifOrientationTest {
    @Test void rotatesBeforeCreatingMasterAndStripsTheOrientationMetadata() throws Exception {
        var image=new BufferedImage(32,16,BufferedImage.TYPE_INT_RGB);var out=new ByteArrayOutputStream();ImageIO.write(image,"jpeg",out);
        byte[] jpeg=out.toByteArray();var metadata=ByteBuffer.allocate(36);
        metadata.put((byte)255).put((byte)225).putShort((short)34).put(new byte[]{'E','x','i','f',0,0});
        metadata.putShort((short)0x4d4d).putShort((short)42).putInt(8).putShort((short)1);
        metadata.putShort((short)0x112).putShort((short)3).putInt(1).putShort((short)6).putShort((short)0).putInt(0);
        out.reset();out.write(jpeg,0,2);out.write(metadata.array());out.write(jpeg,2,jpeg.length-2);
        var result=new SynchronousImageProcessor().process(out.toByteArray(),SynchronousImageProcessor.Category.PROFILE,List.of());
        assertThat(result.master().width()).isEqualTo(16);assertThat(result.master().height()).isEqualTo(32);
        assertThat(ExifOrientation.read(result.master().bytes())).isEqualTo(1);
    }
    @Test void allOrientationsPreservePixelsWithoutReadingOutsideImage() {
        var source=new BufferedImage(3,2,BufferedImage.TYPE_INT_RGB);int color=1;
        for(int y=0;y<2;y++) for(int x=0;x<3;x++) source.setRGB(x,y,color++);
        for(int orientation=1;orientation<=8;orientation++) {
            var output=ExifOrientation.apply(source,orientation);var colors=new java.util.HashSet<Integer>();
            for(int y=0;y<output.getHeight();y++) for(int x=0;x<output.getWidth();x++) colors.add(output.getRGB(x,y));
            assertThat(colors).hasSize(6);
        }
    }
}
