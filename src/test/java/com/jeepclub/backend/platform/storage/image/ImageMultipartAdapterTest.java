package com.jeepclub.backend.platform.storage.image;

import com.jeepclub.backend.shared.storage.exception.InvalidStorageFileException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ImageMultipartAdapterTest {
    @Test void absentAndValidImagesKeepTheirContract() {
        assertThat(ImageMultipartAdapter.read(null)).isNull();
        var image = ImageMultipartAdapter.read(new MockMultipartFile("photo", "image.png", "image/png", new byte[]{1}));
        assertThat(image.filename()).isEqualTo("image.png");
        assertThat(image.contentType()).isEqualTo("image/png");
        assertThat(image.content()).containsExactly((byte) 1);
    }

    @Test void emptyAndOversizedImagesAreRejectedBeforeReadingBytes() throws Exception {
        for (long size : new long[]{0, ImageUpload.MAX_BYTES + 1L}) {
            var file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(size == 0); when(file.getSize()).thenReturn(size);
            assertThatThrownBy(() -> ImageMultipartAdapter.read(file)).isInstanceOf(InvalidStorageFileException.class);
            verify(file, never()).getBytes();
        }
    }

    @Test void onlyByteReadIOExceptionIsTranslatedWithoutRetainingSensitiveCause() throws Exception {
        var file = mock(MultipartFile.class); when(file.getSize()).thenReturn(1L);
        when(file.getBytes()).thenThrow(new IOException("sensitive-temp-path-filename-storageKey-bytes"));
        assertThatThrownBy(() -> ImageMultipartAdapter.read(file)).isInstanceOf(ImageUploadReadException.class)
                .hasMessage("Could not read the uploaded image.").hasNoCause()
                .hasStackTraceContaining("ImageUploadReadException");
    }

    @Test void unrelatedRuntimeFailuresAreNotRelabeledAsRequestReadFailures() throws Exception {
        var file = mock(MultipartFile.class); when(file.getSize()).thenReturn(1L);
        var original = new IllegalStateException("synthetic non-IO failure");
        when(file.getBytes()).thenThrow(original);
        assertThatThrownBy(() -> ImageMultipartAdapter.read(file)).isSameAs(original);
    }
}
