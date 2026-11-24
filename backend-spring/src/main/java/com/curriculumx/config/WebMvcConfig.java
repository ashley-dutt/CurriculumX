package com.curriculumx.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        File cur = new File(".").getAbsoluteFile();
        File staticDir = new File(cur, "static");
        if (!staticDir.exists() && cur.getParentFile() != null) {
            staticDir = new File(cur.getParentFile(), "static");
        }
        File dataDir = new File(cur, "data");
        if (!dataDir.exists() && cur.getParentFile() != null) {
            dataDir = new File(cur.getParentFile(), "data");
        }

        registry.addResourceHandler("/static/**")
                .addResourceLocations(staticDir.toURI().toString());

        registry.addResourceHandler("/data/**")
                .addResourceLocations(dataDir.toURI().toString());
    }
}
