package com.inhatc.demp.config;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
@Configuration
@Profile("local | filesystem")
public class LocalImageConfiguration implements WebMvcConfigurer {
    @Value("${app.local.upload-dir:.local/uploads}") private String directory;
    @Override public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(directory).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/local-files/**").addResourceLocations(location.endsWith("/") ? location : location + "/");
    }
}
