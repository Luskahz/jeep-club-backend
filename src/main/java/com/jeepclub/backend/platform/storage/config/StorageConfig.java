package com.jeepclub.backend.platform.storage.config;

import com.jeepclub.backend.platform.storage.local.LocalFileStorage;
import com.jeepclub.backend.platform.storage.properties.StorageProperties;
import com.jeepclub.backend.shared.storage.FileStorage;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    @Bean
    public FileStorage fileStorage(StorageProperties properties, Clock clock) {
        return switch (properties.provider()) {
            case LOCAL -> new LocalFileStorage(properties, clock);
        };
    }
}
