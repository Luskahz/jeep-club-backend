package com.jeepclub.backend.platform.web.cors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer(CorsProperties properties) {
        return new WebMvcConfigurer() {

            @Override
            public void addCorsMappings(CorsRegistry registry) {

                registry.addMapping("/**")
                        .allowedOrigins(properties.getAllowedOrigins().toArray(String[]::new))
                        .allowedMethods(properties.getAllowedMethods().toArray(String[]::new))
                        .allowedHeaders(properties.getAllowedHeaders().toArray(String[]::new))
                        .allowCredentials(properties.getAllowCredentials())
                        .maxAge(properties.getMaxAge());
            }
        };
    }
}
