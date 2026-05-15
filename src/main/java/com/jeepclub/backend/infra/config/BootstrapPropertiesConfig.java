package com.jeepclub.backend.infra.config;

import com.jeepclub.backend.infra.startup.properties.AdminBootstrapProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AdminBootstrapProperties.class)
public class BootstrapPropertiesConfig {
}