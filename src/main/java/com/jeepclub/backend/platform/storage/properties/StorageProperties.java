package com.jeepclub.backend.platform.storage.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;

@Validated
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    @NotNull
    private Provider provider = Provider.LOCAL;

    @Valid
    @NotNull
    private Local local = new Local();

    public Provider provider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public Local local() {
        return local;
    }

    public void setLocal(Local local) {
        this.local = local;
    }

    public enum Provider {
        LOCAL
    }

    public static class Local {

        @NotNull
        private Path rootDirectory = Path.of("./storage");

        public Path rootDirectory() {
            return rootDirectory;
        }

        public void setRootDirectory(Path rootDirectory) {
            this.rootDirectory = rootDirectory;
        }
    }
}
