package com.jeepclub.backend.storage.api.http;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.*;

@Configuration @ConditionalOnProperty(name="storage.media.enabled",havingValue="true")
public class StorageCacheConfiguration implements WebMvcConfigurer {
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object handler) {
                response.setHeader("Cache-Control","private, no-store");return true;
            }
        }).addPathPatterns("/storage/**","/admin/storage/**");
    }
}
