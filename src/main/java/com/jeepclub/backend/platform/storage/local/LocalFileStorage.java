package com.jeepclub.backend.platform.storage.local;

import com.jeepclub.backend.platform.storage.properties.StorageProperties;
import com.jeepclub.backend.shared.storage.FileStorage;
import com.jeepclub.backend.shared.storage.StorageFile;
import com.jeepclub.backend.shared.storage.StorageResource;
import com.jeepclub.backend.shared.storage.StoredFile;
import com.jeepclub.backend.shared.storage.exception.InvalidStorageFileException;
import com.jeepclub.backend.shared.storage.exception.InvalidStorageKeyException;
import com.jeepclub.backend.shared.storage.exception.InvalidStorageNamespaceException;
import com.jeepclub.backend.shared.storage.exception.StorageCollisionException;
import com.jeepclub.backend.shared.storage.exception.StorageObjectNotFoundException;
import com.jeepclub.backend.shared.storage.exception.StorageOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class LocalFileStorage implements FileStorage {

    private static final Pattern LOGICAL_SEGMENT = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");
    private static final Pattern NORMALIZED_EXTENSION = Pattern.compile("[a-z0-9][a-z0-9_-]{0,31}");
    private static final Pattern WINDOWS_ABSOLUTE_PATH = Pattern.compile("^[A-Za-z]:.*");

    private final Path rootDirectory;
    private final Clock clock;
    private final Supplier<UUID> uuidGenerator;

    public LocalFileStorage(StorageProperties properties, Clock clock) {
        this(properties.local().rootDirectory(), clock, UUID::randomUUID);
    }

    LocalFileStorage(Path rootDirectory, Clock clock, Supplier<UUID> uuidGenerator) {
        this.rootDirectory = Objects.requireNonNull(rootDirectory, "rootDirectory cannot be null")
                .toAbsolutePath()
                .normalize();
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
        this.uuidGenerator = Objects.requireNonNull(uuidGenerator, "uuidGenerator cannot be null");
    }

    @Override
    public StoredFile store(StorageFile file, String namespace) {
        validateFile(file);
        validateNamespace(namespace);

        String storageKey = generateStorageKey(namespace, file.extension());
        Path target = resolveStorageKey(storageKey);

        try {
            createSafeDirectories(target.getParent());
        } catch (IOException exception) {
            throw new StorageOperationException(StorageOperationException.Operation.WRITE, exception);
        }

        try {
            Files.write(
                    target,
                    file.content(),
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
            );
            return new StoredFile(storageKey);
        } catch (FileAlreadyExistsException exception) {
            throw new StorageCollisionException();
        } catch (IOException exception) {
            throw new StorageOperationException(StorageOperationException.Operation.WRITE, exception);
        }
    }

    @Override
    public StorageResource load(String storageKey) {
        Path target = resolveStorageKey(storageKey);
        requireRegularFile(target);

        try (InputStream input = Files.newInputStream(
                target,
                StandardOpenOption.READ,
                LinkOption.NOFOLLOW_LINKS
        )) {
            return new StorageResource(storageKey, input.readAllBytes());
        } catch (NoSuchFileException exception) {
            throw new StorageObjectNotFoundException();
        } catch (IOException exception) {
            throw new StorageOperationException(StorageOperationException.Operation.READ, exception);
        }
    }

    @Override
    public void delete(String storageKey) {
        Path target = resolveStorageKey(storageKey);
        requireRegularFile(target);

        try {
            Files.delete(target);
        } catch (NoSuchFileException exception) {
            throw new StorageObjectNotFoundException();
        } catch (IOException exception) {
            throw new StorageOperationException(StorageOperationException.Operation.DELETE, exception);
        }
    }

    private void validateFile(StorageFile file) {
        if (file == null) {
            throw new InvalidStorageFileException("Storage file is required.");
        }
        if (file.content() == null) {
            throw new InvalidStorageFileException("Storage file content is required.");
        }
        if (file.extension() == null || !NORMALIZED_EXTENSION.matcher(file.extension()).matches()) {
            throw new InvalidStorageFileException("Storage file extension is technically invalid.");
        }
    }

    private void validateNamespace(String namespace) {
        if (!isValidLogicalPath(namespace)) {
            throw new InvalidStorageNamespaceException("Storage namespace is technically invalid.");
        }
    }

    private Path resolveStorageKey(String storageKey) {
        if (!isValidLogicalPath(storageKey)) {
            throw new InvalidStorageKeyException("Storage key is technically invalid.");
        }

        Path target = rootDirectory.resolve(storageKey).normalize();
        if (!target.startsWith(rootDirectory) || target.equals(rootDirectory)) {
            throw new InvalidStorageKeyException("Storage key resolves outside storage root.");
        }

        rejectSymbolicLinks(target, true);
        return target;
    }

    private boolean isValidLogicalPath(String value) {
        if (value == null
                || value.isBlank()
                || value.startsWith("/")
                || value.startsWith("\\")
                || value.contains("\\")
                || WINDOWS_ABSOLUTE_PATH.matcher(value).matches()) {
            return false;
        }

        String[] segments = value.split("/", -1);
        for (String segment : segments) {
            if (segment.isEmpty()
                    || segment.equals(".")
                    || segment.equals("..")
                    || !LOGICAL_SEGMENT.matcher(segment).matches()) {
                return false;
            }
        }
        return true;
    }

    private String generateStorageKey(String namespace, String extension) {
        LocalDate today = LocalDate.now(clock);
        UUID identifier = Objects.requireNonNull(uuidGenerator.get(), "generated UUID cannot be null");

        return "%s/%04d/%02d/%02d/%s.%s".formatted(
                namespace,
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth(),
                identifier,
                extension
        );
    }

    private void createSafeDirectories(Path directory) throws IOException {
        rejectSymbolicLinks(directory, true);
        Files.createDirectories(directory);
        rejectSymbolicLinks(directory, true);

        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Storage directory is unavailable.");
        }
    }

    private void requireRegularFile(Path target) {
        rejectSymbolicLinks(target, true);
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)
                || !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
            throw new StorageObjectNotFoundException();
        }
    }

    private void rejectSymbolicLinks(Path target, boolean includeTarget) {
        if (Files.isSymbolicLink(rootDirectory)) {
            throw new InvalidStorageKeyException("Storage path contains a symbolic link.");
        }

        Path current = rootDirectory;
        Path relative = rootDirectory.relativize(target);
        for (Path segment : relative) {
            current = current.resolve(segment);
            if (!includeTarget && current.equals(target)) {
                break;
            }
            if (Files.isSymbolicLink(current)) {
                throw new InvalidStorageKeyException("Storage path contains a symbolic link.");
            }
        }
    }
}
