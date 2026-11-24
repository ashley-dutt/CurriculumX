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
        // Point static resources to parent workspace directory
        File parentDir = new File("..").getAbsoluteFile();
        String parentPath = parentDir.toURI().toString();

        registry.addResourceHandler("/static/**")
                .addResourceLocations(parentPath + "static/");

        registry.addResourceHandler("/data/**")
                .addResourceLocations(parentPath + "data/");
    }
}
