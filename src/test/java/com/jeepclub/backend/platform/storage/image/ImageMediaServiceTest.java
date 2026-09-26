package com.jeepclub.backend.platform.storage.image;

import com.jeepclub.backend.shared.storage.FileStorage;
import com.jeepclub.backend.shared.storage.StorageFile;
import com.jeepclub.backend.shared.storage.StorageResource;
import com.jeepclub.backend.shared.storage.StoredFile;
import com.jeepclub.backend.shared.storage.exception.InvalidStorageFileException;
import com.jeepclub.backend.shared.storage.exception.InvalidStorageKeyException;
import com.jeepclub.backend.shared.storage.exception.StorageObjectNotFoundException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImageMediaServiceTest {
    private static final String KEY = "images/2026/09/24/550e8400-e29b-41d4-a716-446655440000.png";
    private final FileStorage storage = mock(FileStorage.class);
    private final ImageMediaService images = new ImageMediaService(storage);

    @Test
    void uploadsValidatedImageThroughGlobalStorageAndResolvesIt() {
        byte[] png = {(byte) 137, 80, 78, 71, 13, 10, 26, 10, 1};
        when(storage.store(any(StorageFile.class), eq("images"))).thenReturn(new StoredFile(KEY));
        when(storage.load(KEY)).thenReturn(new StorageResource(KEY, png));

        assertThat(images.store("portrait.png", "image/png", png)).isEqualTo(KEY);
        assertThat(images.requireExisting(KEY)).isEqualTo(KEY);
        assertThat(images.load(KEY).content()).containsExactly(png);
        verify(storage).store(any(StorageFile.class), eq("images"));
    }

    @Test
    void rejectsTransportUrlAndMissingObjectBeforeAssociation() {
        assertThatThrownBy(() -> images.requireExisting("https://example.com/image.png"))
                .isInstanceOf(InvalidStorageKeyException.class);
        when(storage.load(KEY)).thenThrow(new StorageObjectNotFoundException());
        assertThatThrownBy(() -> images.requireExisting(KEY))
                .isInstanceOf(StorageObjectNotFoundException.class);
    }

    @Test
    void rejectsSpoofedContentAndOversizedImages() {
        assertThatThrownBy(() -> images.store("portrait.png", "image/png", new byte[]{1, 2, 3}))
                .isInstanceOf(InvalidStorageFileException.class);
        assertThatThrownBy(() -> images.store("portrait.jpg", "image/jpeg", new byte[5 * 1024 * 1024 + 1]))
                .isInstanceOf(InvalidStorageFileException.class);
    }
}
