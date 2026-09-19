package com.jeepclub.backend.platform.storage.config;

import com.jeepclub.backend.platform.storage.properties.StorageProperties;
import com.jeepclub.backend.platform.storage.local.LocalFileStorage;
import com.jeepclub.backend.platform.time.TimeConfig;
import com.jeepclub.backend.shared.storage.FileStorage;
import com.jeepclub.backend.shared.storage.StorageFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class StorageConfigTest {

    @TempDir
    Path tempDirectory;

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(StorageConfig.class, TimeConfig.class);

    @Test
    void shouldLoadLocalProviderDefaults() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(StorageProperties.class);
            assertThat(context).hasSingleBean(FileStorage.class);
            assertThat(context.getBean(FileStorage.class)).isInstanceOf(LocalFileStorage.class);

            StorageProperties properties = context.getBean(StorageProperties.class);
            assertThat(properties.provider()).isEqualTo(StorageProperties.Provider.LOCAL);
            assertThat(properties.local().rootDirectory()).isEqualTo(Path.of("./storage"));
        });
    }

    @Test
    void shouldBindConfiguredLocalRootDirectory() {
        Path configuredRoot = tempDirectory.resolve("configured-storage");
        contextRunner
                .withPropertyValues(
                        "storage.provider=local",
                        "storage.local.root-directory=" + configuredRoot.toString().replace('\\', '/')
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(StorageProperties.class).local().rootDirectory())
                            .isEqualTo(configuredRoot);

                    FileStorage storage = context.getBean(FileStorage.class);
                    var stored = storage.store(
                            new StorageFile("ignored", "application/octet-stream", "bin", new byte[]{1, 2}),
                            "context/test"
                    );
                    assertThat(Files.readAllBytes(configuredRoot.resolve(stored.storageKey())))
                            .containsExactly(1, 2);
                });
    }

    @Test
    void shouldFailClearlyForUnsupportedProvider() {
        contextRunner
                .withPropertyValues("storage.provider=s3")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalArgumentException.class)
                            .rootCause()
                            .hasMessageContaining("StorageProperties.Provider.s3");
                });
    }
}
