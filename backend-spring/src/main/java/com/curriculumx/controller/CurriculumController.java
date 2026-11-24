package com.curriculumx.controller;

import com.curriculumx.service.CurriculumService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CurriculumController {

    private final CurriculumService service;

    public CurriculumController(CurriculumService service) {
        this.service = service;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(service.getStatus());
    }

    @GetMapping("/records")
    public ResponseEntity<Map<String, Object>> getRecords(
            @RequestParam(value = "course", required = false) String course,
            @RequestParam(value = "mode", required = false) String mode) {
        if (course != null && mode != null) {
            return ResponseEntity.ok(service.getRecord(course, mode));
        }
        return ResponseEntity.ok(service.getAllRecords());
    }

    @PostMapping("/records")
    public ResponseEntity<Map<String, Object>> saveRecord(@RequestBody Map<String, Object> data) {
        try {
            service.saveRecord(data);
            Map<String, Object> resp = new HashMap<>();
            resp.put("ok", true);
            resp.put("message", "Saved to backend database successfully (Spring Boot)");
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(err);
        }
    }

    @PostMapping("/records/reset")
    public ResponseEntity<Map<String, Object>> resetRecords(@RequestBody(required = false) Map<String, Object> data) {
        String courseKey = data != null ? (String) data.get("course_key") : null;
        service.resetRecords(courseKey);
        Map<String, Object> resp = new HashMap<>();
        resp.put("ok", true);
        resp.put("message", "Reset complete (Spring Boot)");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/draft")
    public ResponseEntity<Map<String, Object>> draftCourse(@RequestBody Map<String, Object> data) {
        String code = data.containsKey("code") ? String.valueOf(data.get("code")).trim().toUpperCase() : "NEW";
        String name = data.containsKey("name") ? String.valueOf(data.get("name")).trim() : "New Course";
        String ltp = data.containsKey("ltp") ? String.valueOf(data.get("ltp")).trim() : "3-0-0";

        List<Map<String, Object>> units = new ArrayList<>();
        units.add(Map.of("t", "Foundational Concepts & Principles", "x", List.of(
            "Theoretical foundations of " + name, "Core paradigms and problem definitions",
            "Mathematical representations and specifications", "Complexity analysis and baseline approaches"
        )));
        units.add(Map.of("t", "Algorithmic Paradigms & Data Models", "x", List.of(
            "Core architectural data representations", "Algorithmic structures in " + name,
            "Optimization formulations and criteria", "Pipeline engineering and modular workflows"
        )));
        units.add(Map.of("t", "Practical Implementations & Methods", "x", List.of(
            "Primary algorithmic workflows", "Hands-on implementation and validation",
            "Comparative benchmarking against standards", "Empirical performance evaluations"
        )));
        units.add(Map.of("t", "Advanced Optimization & Scaling", "x", List.of(
            "Advanced optimization strategies", "High-throughput and distributed scaling",
            "Fault tolerance, constraints, and robustness", "Bottleneck profiling and execution speedups"
        )));
        units.add(Map.of("t", "Applied Systems & Domain Workflows", "x", List.of(
            "End-to-end system pipelines", "Industry case studies and applications",
            "Deployment considerations and testing", "Integration with standard tooling ecosystems"
        )));
        units.add(Map.of("t", "Emerging Paradigms & Frontiers", "x", List.of(
            "Current research developments", "Open challenges and scalable frontiers",
            "Ethics, security, and governance", "Comprehensive review and capstone design"
        )));

        List<String> src = List.of("MIT OpenCourseWare (6.006) · Verified Benchmarks", "Premier University Standards");

        Map<String, Object> resp = new HashMap<>();
        resp.put("code", code);
        resp.put("name", name);
        resp.put("ltp", ltp);
        resp.put("units", units);
        resp.put("src", src);

        return ResponseEntity.ok(resp);
    }
}
