package com.ucmp.ucmp_backend.service;

import com.ucmp.ucmp_backend.repository.AnnouncementRepository;
import com.ucmp.ucmp_backend.repository.ClassCancellationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * CleanupSchedulerService
 * ─────────────────────────────────────────────────────────────
 * Runs automatically in the background to purge stale records
 * that accumulate over time and slow down the application.
 *
 * Rules:
 * - Announcements (notifications / alerts):
 * Deleted after 24 hours from their createdAt timestamp.
 * These are ephemeral push-style notifications; there is
 * no value in retaining them beyond the day.
 *
 * - ClassCancellations:
 * Purged when the cancellation date is more than 7 days
 * in the past. Past cancellations only serve as display
 * history and have zero effect on attendance calculations.
 *
 * - NOT touched:
 * AttendanceSessions, AttendanceRecords, TimetableEntries,
 * TimetableOverrides, Users, Messages (E2E conversations).
 * Those are permanent academic records.
 *
 * Schedule: every hour, on the hour.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CleanupSchedulerService {

    private final AnnouncementRepository announcementRepository;
    private final ClassCancellationRepository classCancellationRepository;

    // ── Thresholds ─────────────────────────────────────────────────────────────
    /** Announcements / notifications older than this many hours are deleted. */
    private static final int ANNOUNCEMENT_TTL_HOURS = 24;

    /** Past class cancellations older than this many days are pruned. */
    private static final int CANCELLATION_TTL_DAYS = 7;

    // ── Job: runs every hour at minute 0 ────────────────────────────────────────
    /**
     * Cron: "0 0 * * * *" → fires at second 0, minute 0, of every hour.
     *
     * Each run is its own transaction so a failure in one delete
     * does not roll back the other.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void runCleanup() {
        log.info("[Cleanup] Starting scheduled database cleanup...");
        purgeStaleAnnouncements();
        purgeOldCancellations();
        log.info("[Cleanup] Scheduled cleanup complete.");
    }

    // ── Step 1: Announcements (strict 24-hour TTL) ──────────────────────────────
    @Transactional
    public void purgeStaleAnnouncements() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(ANNOUNCEMENT_TTL_HOURS);
        try {
            int deleted = announcementRepository.deleteByCreatedAtBefore(cutoff);
            if (deleted > 0) {
                log.info("[Cleanup] Announcements: deleted {} record(s) older than {} hours.",
                        deleted, ANNOUNCEMENT_TTL_HOURS);
            } else {
                log.debug("[Cleanup] Announcements: nothing to purge.");
            }
        } catch (Exception e) {
            log.error("[Cleanup] Announcements purge failed: {}", e.getMessage(), e);
        }
    }

    // ── Step 2: Class Cancellations (7-day historical buffer) ──────────────────
    @Transactional
    public void purgeOldCancellations() {
        LocalDate cutoffDate = LocalDate.now().minusDays(CANCELLATION_TTL_DAYS);
        try {
            int deleted = classCancellationRepository.deleteOlderThan(cutoffDate);
            if (deleted > 0) {
                log.info("[Cleanup] ClassCancellations: deleted {} record(s) with date before {}.",
                        deleted, cutoffDate);
            } else {
                log.debug("[Cleanup] ClassCancellations: nothing to purge.");
            }
        } catch (Exception e) {
            log.error("[Cleanup] ClassCancellations purge failed: {}", e.getMessage(), e);
        }
    }
}
