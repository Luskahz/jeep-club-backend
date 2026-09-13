package com.jeepclub.backend.platform.storage.config;

import com.jeepclub.backend.platform.storage.properties.StorageProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class StorageConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(StorageConfig.class);

    @Test
    void shouldLoadLocalProviderDefaults() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(StorageProperties.class);

            StorageProperties properties = context.getBean(StorageProperties.class);
            assertThat(properties.provider()).isEqualTo(StorageProperties.Provider.LOCAL);
            assertThat(properties.local().rootDirectory()).isEqualTo(Path.of("./storage"));
        });
    }

    @Test
    void shouldBindConfiguredLocalRootDirectory() {
        contextRunner
                .withPropertyValues(
                        "storage.provider=local",
                        "storage.local.root-directory=build/test-storage"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(StorageProperties.class).local().rootDirectory())
                            .isEqualTo(Path.of("build/test-storage"));
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
