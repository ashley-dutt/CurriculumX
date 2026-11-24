package com.curriculumx.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.File;

@Controller
public class StaticFileController {

    @GetMapping(value = {"/", "/index.html"}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> serveIndex() {
        File file = new File("index.html");
        if (!file.exists()) {
            file = new File("../index.html");
        }
        if (!file.exists()) {
            file = new File("Curriculum_X/index.html");
        }
        if (file.exists()) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .body(new FileSystemResource(file));
        }
        return ResponseEntity.notFound().build();
    }
}
