package com.jeepclub.backend.platform.security.config;

import com.jeepclub.backend.platform.security.filter.JwtAuthenticationFilter;
import com.jeepclub.backend.platform.security.filter.FrontendAccessFilter;
import com.jeepclub.backend.platform.logging.RequestContextEnrichmentFilter;
import com.jeepclub.backend.platform.security.jwt.JwtProperties;
import com.jeepclub.backend.platform.security.handler.ApiAccessDeniedHandler;
import com.jeepclub.backend.platform.security.handler.ApiAuthenticationEntryPoint;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final FrontendAccessFilter frontendAccessFilter;
    private final ApiAuthenticationEntryPoint authenticationEntryPoint;
    private final ApiAccessDeniedHandler accessDeniedHandler;
    private final RequestContextEnrichmentFilter requestContextEnrichmentFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            FrontendAccessFilter frontendAccessFilter,
            ApiAuthenticationEntryPoint authenticationEntryPoint,
            ApiAccessDeniedHandler accessDeniedHandler,
            RequestContextEnrichmentFilter requestContextEnrichmentFilter
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.frontendAccessFilter = frontendAccessFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.requestContextEnrichmentFilter = requestContextEnrichmentFilter;
    }

    @Bean
    public FilterRegistrationBean<FrontendAccessFilter> frontendAccessFilterRegistration() {
        var registration = new FilterRegistrationBean<>(frontendAccessFilter);
        registration.setEnabled(false); // Only run inside the Spring Security chain.
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(FrontendAccessFilter::isInternalHealth).permitAll()
                        .requestMatchers(
                                "/authentication/login",
                                "/identity/register",
                                "/authentication/refresh",
                                "/authentication/login/password-change",

                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/openapi-custom/**",
                                "/docs/**",

                                "/error"


                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/membership-applications"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/membership-applications/activate"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/membership-applications/activate"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/authentication/password-recovery/requests",
                                "/authentication/password-recovery/requests/email-token",
                                "/authentication/password-recovery/requests/token/reset"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .addFilterBefore(frontendAccessFilter, CorsFilter.class)
                .addFilterAfter(
                        requestContextEnrichmentFilter,
                        JwtAuthenticationFilter.class
                );

        return http.build();
    }
}
