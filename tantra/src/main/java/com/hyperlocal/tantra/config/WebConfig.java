package com.hyperlocal.tantra.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Serves uploaded images from the local upload folder under the configured base URL
 * (e.g. GET /files/&lt;name&gt;.jpg). Replace with a CDN base URL when moving to cloud storage.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.upload.base-url:/files}")
    private String baseUrl;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Only wire a static handler for a local path prefix (not for a full CDN URL).
        if (!baseUrl.startsWith("/")) {
            return;
        }
        String location = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        String pattern = (baseUrl.endsWith("/") ? baseUrl : baseUrl + "/") + "**";
        registry.addResourceHandler(pattern).addResourceLocations(location);
    }
}
