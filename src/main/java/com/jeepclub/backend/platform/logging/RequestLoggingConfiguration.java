package com.jeepclub.backend.platform.logging;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class RequestLoggingConfiguration {

    @Bean
    FilterRegistrationBean<RequestContextEnrichmentFilter> disableStandaloneEnrichmentRegistration(
            RequestContextEnrichmentFilter filter
    ) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
