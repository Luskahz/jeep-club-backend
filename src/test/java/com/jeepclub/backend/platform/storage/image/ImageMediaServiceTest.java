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
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("additionalValidFormats")
    void validatesAndCanonicalizesJpegAndWebpBeforeStoring(String filename, String type, String extension, byte[] bytes) {
        String storedKey=KEY.replace(".png","."+extension);
        when(storage.store(any(StorageFile.class), eq("images"))).thenReturn(new StoredFile(storedKey));
        assertThat(images.store(filename, type.toUpperCase(java.util.Locale.ROOT), bytes)).isEqualTo(storedKey);
        assertThat(ImageMediaService.contentType(storedKey)).isEqualTo(type);
        verify(storage).store(org.mockito.ArgumentMatchers.argThat(file -> file.contentType().equals(type)
                && file.extension().equals(extension) && java.util.Arrays.equals(file.content(), bytes)), eq("images"));
    }
    static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> additionalValidFormats() {
        return java.util.stream.Stream.of(
                org.junit.jupiter.params.provider.Arguments.of("portrait.JPEG", "image/jpeg", "jpg", new byte[]{(byte)255,(byte)216,(byte)255}),
                org.junit.jupiter.params.provider.Arguments.of("portrait.webp", "image/webp", "webp", "RIFF0000WEBP".getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("invalidImages")
    void invalidMetadataMimeOrSignatureNeverTouchesStorage(String filename, String type, byte[] bytes) {
        assertThatThrownBy(() -> images.store(filename, type, bytes)).isInstanceOf(InvalidStorageFileException.class);
        org.mockito.Mockito.verifyNoInteractions(storage);
    }
    static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> invalidImages() {
        return java.util.stream.Stream.of(
                org.junit.jupiter.params.provider.Arguments.of(null,"image/png",new byte[]{1}),
                org.junit.jupiter.params.provider.Arguments.of("photo.png",null,new byte[]{1}),
                org.junit.jupiter.params.provider.Arguments.of("photo.png","image/png",null),
                org.junit.jupiter.params.provider.Arguments.of("photo.png","image/png",new byte[0]),
                org.junit.jupiter.params.provider.Arguments.of("photo","image/png",new byte[]{1}),
                org.junit.jupiter.params.provider.Arguments.of("photo.gif","image/gif",new byte[]{1}),
                org.junit.jupiter.params.provider.Arguments.of("photo.png","image/jpeg",new byte[]{(byte)137,80,78,71,13,10,26,10}),
                org.junit.jupiter.params.provider.Arguments.of("photo.jpg","image/jpeg",new byte[]{(byte)255,(byte)216}),
                org.junit.jupiter.params.provider.Arguments.of("photo.jpg","image/jpeg",new byte[]{0,(byte)216,(byte)255}),
                org.junit.jupiter.params.provider.Arguments.of("photo.jpg","image/jpeg",new byte[]{(byte)255,0,(byte)255}),
                org.junit.jupiter.params.provider.Arguments.of("photo.jpg","image/jpeg",new byte[]{(byte)255,(byte)216,0}),
                org.junit.jupiter.params.provider.Arguments.of("photo.webp","image/webp","RIFF0000WEB".getBytes(java.nio.charset.StandardCharsets.US_ASCII)),
                org.junit.jupiter.params.provider.Arguments.of("photo.webp","image/webp","FAIL0000WEBP".getBytes(java.nio.charset.StandardCharsets.US_ASCII)),
                org.junit.jupiter.params.provider.Arguments.of("photo.webp","image/webp","RIFF0000FAIL".getBytes(java.nio.charset.StandardCharsets.US_ASCII)),
                org.junit.jupiter.params.provider.Arguments.of("photo.png","image/png",new byte[]{(byte)137,80,78,71,13,10,26,0}));
    }
    @Test void acceptsExactlyTheImageSizeLimitAndAbsenceOfAnOptionalAssociationDoesNotReadStorage() {
        byte[] bytes = new byte[ImageUpload.MAX_BYTES];
        System.arraycopy(new byte[]{(byte)137,80,78,71,13,10,26,10},0,bytes,0,8);
        when(storage.store(any(StorageFile.class), eq("images"))).thenReturn(new StoredFile(KEY));
        assertThat(images.store("photo.png","image/png",bytes)).isEqualTo(KEY);
        assertThat(images.requireExisting(null)).isNull();
        verify(storage, org.mockito.Mockito.never()).load(org.mockito.ArgumentMatchers.any());
    }
    private static final String KEY = "images/2026/09/24/550e8400-e29b-41d4-a716-446655440000.png";
    private final FileStorage storage = mock(FileStorage.class);
    private final ImageMediaService images = new ImageMediaService(storage);

    @Test
    void uploadsValidatedImageThroughGlobalStorageAndResolvesIt() {
        byte[] png = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
        when(storage.store(any(StorageFile.class), eq("images"))).thenReturn(new StoredFile(KEY));
        when(storage.load(KEY)).thenReturn(new StorageResource(KEY, png));

        assertThat(images.store("portrait.png", "image/png", png)).isEqualTo(KEY);
        assertThat(images.requireExisting(KEY)).isEqualTo(KEY);
        assertThat(images.load(KEY).content()).containsExactly(png);
        assertThat(ImageMediaService.contentType(KEY)).isEqualTo("image/png");
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
