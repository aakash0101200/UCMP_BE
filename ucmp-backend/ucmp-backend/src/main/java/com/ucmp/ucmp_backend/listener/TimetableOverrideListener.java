package com.ucmp.ucmp_backend.listener;

import com.ucmp.ucmp_backend.event.TimetableOverrideEvent;
import com.ucmp.ucmp_backend.model.Announcements;
import com.ucmp.ucmp_backend.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class TimetableOverrideListener {

    private final AnnouncementRepository announcementRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final CacheManager cacheManager;

    @EventListener
    public void handleTimetableOverride(TimetableOverrideEvent event) {
        // Targeted cache eviction — only invalidate affected sections and faculties
        evictAffectedCaches(event);

        if (event.getSectionIds() == null || event.getSectionIds().isEmpty()) {
            return;
        }

        String typeDesc = event.getOverrideType();
        String title = "Timetable Update: " + typeDesc;
        String desc = String.format("A scheduling change has occurred for your section on %s. Type: %s. Reason: %s",
                event.getDate().toString(), typeDesc, event.getReason());

        for (Long sectionId : event.getSectionIds()) {
            try {
                Announcements announcement = new Announcements();
                announcement.setTitle(title);
                announcement.setDescription(desc);
                announcement.setAuthor("Academic Operations");
                announcement.setTime(LocalDateTime.now().toString());
                announcement.setType("SCHEDULE_OVERRIDE");
                announcement.setSectionId(sectionId);
                announcement.setCompleted(false);

                announcementRepository.save(announcement);

                // Broadcast to notifications channel for real-time delivery
                messagingTemplate.convertAndSend("/topic/notifications/section/" + sectionId, announcement);
            } catch (Exception e) {
                System.err.println("Failed to broadcast timetable override notification: " + e.getMessage());
            }
        }
    }

    /**
     * Evicts only the cached schedules for the sections and faculties affected by this override event,
     * rather than wiping all cached schedules globally.
     */
    private void evictAffectedCaches(TimetableOverrideEvent event) {
        String dateStr = event.getDate().toString();

        // Evict section schedule caches for affected sections
        Cache sectionCache = cacheManager.getCache("resolved_section_schedules");
        if (sectionCache != null && event.getSectionIds() != null) {
            for (Long sectionId : event.getSectionIds()) {
                sectionCache.evict(sectionId + ":" + dateStr);
            }
        }

        // Evict faculty schedule caches for affected faculties
        Cache facultyCache = cacheManager.getCache("resolved_faculty_schedules");
        if (facultyCache != null) {
            if (event.getOriginalFacultyId() != null) {
                facultyCache.evict(event.getOriginalFacultyId() + ":" + dateStr);
            }
            if (event.getNewFacultyId() != null) {
                facultyCache.evict(event.getNewFacultyId() + ":" + dateStr);
            }
        }
    }
}
