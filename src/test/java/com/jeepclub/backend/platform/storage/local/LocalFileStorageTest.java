package com.jeepclub.backend.platform.storage.local;

import com.jeepclub.backend.shared.storage.StorageFile;
import com.jeepclub.backend.shared.storage.exception.InvalidStorageFileException;
import com.jeepclub.backend.shared.storage.exception.InvalidStorageKeyException;
import com.jeepclub.backend.shared.storage.exception.InvalidStorageNamespaceException;
import com.jeepclub.backend.shared.storage.exception.StorageCollisionException;
import com.jeepclub.backend.shared.storage.exception.StorageObjectNotFoundException;
import com.jeepclub.backend.shared.storage.exception.StorageOperationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageTest {

    private static final UUID FIXED_UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-09-13T15:30:00Z"),
            ZoneOffset.UTC
    );

    @TempDir
    Path tempDirectory;

    @Test
    void shouldStoreLoadAndDeleteFileUsingPortableStorageKey() throws IOException {
        Path root = tempDirectory.resolve("root");
        LocalFileStorage storage = storage(root);
        byte[] content = "exact-content".getBytes(StandardCharsets.UTF_8);
        StorageFile file = new StorageFile("../../client-name.pdf", "application/pdf", "pdf", content);

        var stored = storage.store(file, "billing/payment-receipts");

        assertThat(stored.storageKey()).isEqualTo(
                "billing/payment-receipts/2026/09/13/550e8400-e29b-41d4-a716-446655440000.pdf"
        );
        assertThat(stored.storageKey()).doesNotContain(root.toString(), "\\", ":");
        assertThat(Files.readAllBytes(root.resolve(stored.storageKey()))).isEqualTo(content);

        var loaded = storage.load(stored.storageKey());
        assertThat(loaded.storageKey()).isEqualTo(stored.storageKey());
        assertThat(loaded.content()).isEqualTo(content);
        assertThat(loaded.size()).isEqualTo(content.length);

        storage.delete(stored.storageKey());
        assertThat(root.resolve(stored.storageKey())).doesNotExist();
    }

    @Test
    void shouldCreateDirectoriesAndStoreZeroByteFile() {
        Path root = tempDirectory.resolve("nested-root");
        LocalFileStorage storage = storage(root);

        var stored = storage.store(new StorageFile(null, null, "bin", new byte[0]), "generic/files");

        assertThat(root.resolve(stored.storageKey())).isRegularFile();
        assertThat(storage.load(stored.storageKey()).content()).isEmpty();
    }

    @Test
    void shouldNotOverwriteWhenGeneratedKeyCollides() throws IOException {
        Path root = tempDirectory.resolve("root");
        LocalFileStorage storage = storage(root);
        StorageFile first = new StorageFile("first", null, "dat", new byte[]{1});
        StorageFile second = new StorageFile("second", null, "dat", new byte[]{2});

        var stored = storage.store(first, "namespace");

        assertThatThrownBy(() -> storage.store(second, "namespace"))
                .isInstanceOf(StorageCollisionException.class);
        assertThat(Files.readAllBytes(root.resolve(stored.storageKey()))).containsExactly(1);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "", " ", ".", "..", "../billing", "..\\billing", "billing/../identity",
            "/billing", "\\billing", "C:\\billing", "C:/billing", "billing//receipts", "billing/"
    })
    void shouldRejectInvalidNamespace(String namespace) {
        LocalFileStorage storage = storage(tempDirectory.resolve("root"));

        assertThatThrownBy(() -> storage.store(validFile(), namespace))
                .isInstanceOf(InvalidStorageNamespaceException.class);
    }

    @ParameterizedTest
    @MethodSource("invalidExtensions")
    void shouldRejectTechnicallyInvalidExtension(String extension) {
        LocalFileStorage storage = storage(tempDirectory.resolve("root"));
        StorageFile file = new StorageFile("ignored", "ignored", extension, new byte[]{1});

        assertThatThrownBy(() -> storage.store(file, "valid/namespace"))
                .isInstanceOf(InvalidStorageFileException.class);
    }

    @Test
    void shouldRejectMissingFileOrContent() {
        LocalFileStorage storage = storage(tempDirectory.resolve("root"));

        assertThatThrownBy(() -> storage.store(null, "namespace"))
                .isInstanceOf(InvalidStorageFileException.class);
        assertThatThrownBy(() -> storage.store(new StorageFile(null, null, "bin", null), "namespace"))
                .isInstanceOf(InvalidStorageFileException.class);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "", " ", ".", "..", "../outside.txt", "..\\outside.txt", "safe/../outside.txt",
            "/absolute/path", "\\absolute\\path", "C:\\absolute\\path", "C:/absolute/path",
            "safe//file.bin", "https://host/file.bin"
    })
    void shouldRejectInvalidKeyOnLoad(String storageKey) {
        LocalFileStorage storage = storage(tempDirectory.resolve("root"));

        assertThatThrownBy(() -> storage.load(storageKey))
                .isInstanceOf(InvalidStorageKeyException.class);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "", " ", "../outside.txt", "..\\outside.txt", "/absolute/path",
            "\\absolute\\path", "C:\\absolute\\path", "C:/absolute/path"
    })
    void shouldRejectInvalidKeyOnDelete(String storageKey) {
        LocalFileStorage storage = storage(tempDirectory.resolve("root"));

        assertThatThrownBy(() -> storage.delete(storageKey))
                .isInstanceOf(InvalidStorageKeyException.class);
    }

    @Test
    void shouldDistinguishMissingObjectAndDirectoryFromReadableFile() throws IOException {
        Path root = tempDirectory.resolve("root");
        Files.createDirectories(root.resolve("directory"));
        LocalFileStorage storage = storage(root);

        assertThatThrownBy(() -> storage.load("missing.bin"))
                .isInstanceOf(StorageObjectNotFoundException.class);
        assertThatThrownBy(() -> storage.delete("missing.bin"))
                .isInstanceOf(StorageObjectNotFoundException.class);
        assertThatThrownBy(() -> storage.load("directory"))
                .isInstanceOf(StorageObjectNotFoundException.class);
        assertThatThrownBy(() -> storage.delete("directory"))
                .isInstanceOf(StorageObjectNotFoundException.class);
    }

    @Test
    void shouldNotDeleteResourceOutsideRoot() throws IOException {
        Path root = tempDirectory.resolve("root");
        Path outside = tempDirectory.resolve("outside.txt");
        Files.writeString(outside, "preserve");
        LocalFileStorage storage = storage(root);

        assertThatThrownBy(() -> storage.delete("../outside.txt"))
                .isInstanceOf(InvalidStorageKeyException.class);
        assertThat(outside).hasContent("preserve");
    }

    @Test
    void shouldMapWriteFailureWithoutExposingPhysicalPath() throws IOException {
        Path rootFile = tempDirectory.resolve("root-is-a-file");
        Files.writeString(rootFile, "not-a-directory");
        LocalFileStorage storage = storage(rootFile);

        assertThatThrownBy(() -> storage.store(validFile(), "namespace"))
                .isInstanceOfSatisfying(StorageOperationException.class, exception -> {
                    assertThat(exception.operation()).isEqualTo(StorageOperationException.Operation.WRITE);
                    assertThat(exception.getMessage()).isEqualTo("Could not store file.");
                    assertThat(exception.getMessage()).doesNotContain(rootFile.toString());
                    assertThat(exception.getCause()).isInstanceOf(IOException.class);
                });
    }

    @Test
    void shouldRejectSymbolicLinkInsteadOfFollowingItOutsideRoot() throws IOException {
        Path root = tempDirectory.resolve("root");
        Path outside = tempDirectory.resolve("outside.txt");
        Files.createDirectories(root);
        Files.writeString(outside, "preserve");
        Path link = root.resolve("linked.txt");

        try {
            Files.createSymbolicLink(link, outside);
        } catch (UnsupportedOperationException | IOException | SecurityException exception) {
            Assumptions.assumeTrue(false, "Symbolic links are unavailable in this test environment");
        }

        LocalFileStorage storage = storage(root);
        assertThatThrownBy(() -> storage.load("linked.txt"))
                .isInstanceOf(InvalidStorageKeyException.class);
        assertThatThrownBy(() -> storage.delete("linked.txt"))
                .isInstanceOf(InvalidStorageKeyException.class);
        assertThat(outside).hasContent("preserve");
    }

    private LocalFileStorage storage(Path root) {
        return new LocalFileStorage(root, FIXED_CLOCK, () -> FIXED_UUID);
    }

    private StorageFile validFile() {
        return new StorageFile("ignored.name", "application/octet-stream", "bin", new byte[]{1});
    }

    private static Stream<String> invalidExtensions() {
        return Stream.of(
                null, "", " ", "PDF", ".pdf", "tar.gz", "../exe", "jpg/..", "j\\pg",
                "with space", "extension-that-is-longer-than-thirty-two-characters"
        );
    }
}
