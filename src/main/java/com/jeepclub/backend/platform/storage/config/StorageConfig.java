package com.jeepclub.backend.platform.storage.config;

import com.jeepclub.backend.platform.storage.properties.StorageProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {
}
