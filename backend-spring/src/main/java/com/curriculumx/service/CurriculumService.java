package com.curriculumx.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;

@Service
public class CurriculumService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public CurriculumService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void initDatabase() {
        try {
            // Ensure data directory exists
            File dataDir = new File("../data");
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS records (
                    course_key TEXT NOT NULL,
                    mode TEXT NOT NULL,
                    scheme TEXT,
                    term TEXT,
                    role TEXT,
                    step INTEGER,
                    gen INTEGER,
                    decisions TEXT,
                    frozen_units TEXT,
                    magic INTEGER,
                    status TEXT,
                    remarks TEXT,
                    alignment_scores TEXT,
                    unit_gaps TEXT,
                    updated_at TEXT,
                    PRIMARY KEY(course_key, mode)
                );
            """);

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS audit_log (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    course_key TEXT,
                    mode TEXT,
                    role TEXT,
                    action TEXT,
                    details TEXT,
                    created_at TEXT
                );
            """);
            System.out.println("✅ SQLite curriculum_records.db initialized successfully!");
        } catch (Exception e) {
            System.err.println("Database init warning: " + e.getMessage());
        }
    }

    public Map<String, Object> getStatus() {
        Map<String, Object> resp = new HashMap<>();
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM records", Integer.class);
            resp.put("status", "ok");
            resp.put("total_records", count != null ? count : 0);
            resp.put("db", "SQLite (curriculum_records.db via Spring Boot JDBC)");
        } catch (Exception e) {
            resp.put("status", "ok");
            resp.put("total_records", 0);
            resp.put("db", "SQLite (curriculum_records.db via Spring Boot JDBC)");
        }
        return resp;
    }

    public Map<String, Object> getRecord(String courseKey, String mode) {
        Map<String, Object> resp = new HashMap<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM records WHERE course_key = ? AND mode = ?", courseKey, mode
            );
            if (!rows.isEmpty()) {
                resp.put("record", rows.get(0));
            } else {
                resp.put("record", null);
            }
        } catch (Exception e) {
            resp.put("record", null);
        }
        return resp;
    }

    public Map<String, Object> getAllRecords() {
        Map<String, Object> resp = new HashMap<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM records ORDER BY updated_at DESC"
            );
            resp.put("records", rows);
        } catch (Exception e) {
            resp.put("records", Collections.emptyList());
        }
        return resp;
    }

    public void saveRecord(Map<String, Object> data) {
        String courseKey = String.valueOf(data.getOrDefault("course_key", "")).trim();
        if (courseKey.isEmpty()) {
            throw new IllegalArgumentException("course_key is required");
        }
        String mode = String.valueOf(data.getOrDefault("mode", "syl"));
        String scheme = String.valueOf(data.getOrDefault("scheme", ""));
        String term = String.valueOf(data.getOrDefault("term", ""));
        String role = String.valueOf(data.getOrDefault("role", "Faculty"));
        int step = data.get("step") instanceof Number ? ((Number) data.get("step")).intValue() : 0;
        int gen = Boolean.TRUE.equals(data.get("gen")) || Integer.valueOf(1).equals(data.get("gen")) ? 1 : 0;
        int magic = Boolean.TRUE.equals(data.get("magic")) || Integer.valueOf(1).equals(data.get("magic")) ? 1 : 0;
        String status = String.valueOf(data.getOrDefault("status", "Draft"));
        String remarks = String.valueOf(data.getOrDefault("remarks", ""));

        String decisions = serializeJson(data.get("decisions"), "{}");
        String frozenUnits = serializeJson(data.get("frozen_units"), "[]");
        String alignmentScores = serializeJson(data.get("alignment_scores"), "{}");
        String unitGaps = serializeJson(data.get("unit_gaps"), "[]");

        String sql = """
            INSERT INTO records (
                course_key, mode, scheme, term, role, step, gen,
                decisions, frozen_units, magic, status, remarks,
                alignment_scores, unit_gaps, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'))
            ON CONFLICT(course_key, mode) DO UPDATE SET
                scheme = excluded.scheme,
                term = excluded.term,
                role = excluded.role,
                step = excluded.step,
                gen = excluded.gen,
                decisions = excluded.decisions,
                frozen_units = excluded.frozen_units,
                magic = excluded.magic,
                status = excluded.status,
                remarks = excluded.remarks,
                alignment_scores = excluded.alignment_scores,
                unit_gaps = excluded.unit_gaps,
                updated_at = datetime('now')
        """;

        jdbcTemplate.update(sql,
            courseKey, mode, scheme, term, role, step, gen,
            decisions, frozenUnits, magic, status, remarks,
            alignmentScores, unitGaps
        );

        // Audit log entry
        String auditSql = """
            INSERT INTO audit_log (course_key, mode, role, action, details, created_at)
            VALUES (?, ?, ?, ?, ?, datetime('now'))
        """;
        jdbcTemplate.update(auditSql,
            courseKey, mode, role,
            String.valueOf(data.getOrDefault("action", "sync_state")),
            "Status: " + status + ", Step: " + step
        );
    }

    public void resetRecords(String courseKey) {
        if (courseKey != null && !courseKey.trim().isEmpty()) {
            jdbcTemplate.update("DELETE FROM records WHERE course_key = ?", courseKey.trim());
        } else {
            jdbcTemplate.update("DELETE FROM records");
        }
    }

    private String serializeJson(Object obj, String defaultVal) {
        if (obj == null) return defaultVal;
        if (obj instanceof String) return (String) obj;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return defaultVal;
        }
    }
}
