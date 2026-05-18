package com.ucmp.ucmp_backend.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SchemaMigrationRunner {

    private final JdbcTemplate jdbcTemplate;

    /**
     * @PostConstruct runs immediately during application startup, BEFORE any API requests.
     * This forces the missing columns to be injected into Neon DB automatically,
     * bypassing Hibernate's NOT NULL restriction limitation.
     */
    @PostConstruct
    public void runMigration() {
        try {
            log.info("====== INJECTING MISSING COLUMNS INTO NEON DB ======");
            
            // 1. Update Sessions
            jdbcTemplate.execute("ALTER TABLE attendance_sessions ADD COLUMN IF NOT EXISTS session_type VARCHAR(20) NOT NULL DEFAULT 'REGULAR'");
            jdbcTemplate.execute("ALTER TABLE attendance_sessions ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'");
            jdbcTemplate.execute("ALTER TABLE attendance_sessions ADD COLUMN IF NOT EXISTS manual_mark_grace_minutes INT NOT NULL DEFAULT 15");

            // 2. Update Records
            jdbcTemplate.execute("ALTER TABLE attendance_records ADD COLUMN IF NOT EXISTS marked_by VARCHAR(20) NOT NULL DEFAULT 'STUDENT_TOTP'");
            jdbcTemplate.execute("ALTER TABLE attendance_records ADD COLUMN IF NOT EXISTS marked_by_faculty_id BIGINT"); // Foreign keys auto-handled by Hibernate later
            jdbcTemplate.execute("ALTER TABLE attendance_records ADD COLUMN IF NOT EXISTS grace_reason VARCHAR(255)");

            log.info("====== DB MIGRATION SUCCESSFUL! ======");
        } catch (Exception e) {
            log.warn("Migration notice (safe to ignore if columns already exist): {}", e.getMessage());
        }
    }
}
