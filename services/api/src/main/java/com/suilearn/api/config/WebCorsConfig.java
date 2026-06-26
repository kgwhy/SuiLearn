package com.suilearn.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Cross-origin configuration for browser clients (the Vite web app).
 *
 * <p>In local development the web app is served from a different origin (http://localhost:5173)
 * and reaches the API through the Vite dev proxy, so CORS is not exercised. Once the web app is
 * deployed to its own origin it talks to the API directly and the browser enforces CORS. Allowed
 * origins are configurable via {@code suilearn.web.cors.allowed-origins} (comma-separated) so
 * production hosts can be added without code changes.
 */
@Configuration
public class WebCorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public WebCorsConfig(
        @Value("${suilearn.web.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
        String[] allowedOrigins
    ) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins(allowedOrigins)
            .allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .maxAge(3600);
    }
}
