package com.jeepclub.backend.storage.infra.blob;

import com.jeepclub.backend.shared.storage.*;
import org.junit.jupiter.api.Test;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class LegacyMediaSourceAdapterTest {
    @Test void identifiesActualMimeWithoutChangingKeyOrWritingObjects() throws Exception {
        var storage=mock(FileStorage.class);var image=new BufferedImage(31,17,BufferedImage.TYPE_INT_RGB);var bytes=new ByteArrayOutputStream();ImageIO.write(image,"png",bytes);
        when(storage.load("images/2026/11/09/550e8400-e29b-41d4-a716-446655440000.jpg")).thenReturn(new StorageResource("images/2026/11/09/550e8400-e29b-41d4-a716-446655440000.jpg",bytes.toByteArray()));
        var source=new LegacyMediaSourceAdapter(storage);var content=source.read("images/2026/11/09/550e8400-e29b-41d4-a716-446655440000.jpg");var metadata=source.identify(content);
        assertThat(metadata.mimeType()).isEqualTo("image/png");assertThat(metadata.width()).isEqualTo(31);assertThat(metadata.height()).isEqualTo(17);
        verify(storage).load("images/2026/11/09/550e8400-e29b-41d4-a716-446655440000.jpg");verifyNoMoreInteractions(storage);
    }
}
