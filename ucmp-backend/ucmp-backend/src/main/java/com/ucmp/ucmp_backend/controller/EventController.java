package com.ucmp.ucmp_backend.controller;

import com.ucmp.ucmp_backend.model.Event;
import com.ucmp.ucmp_backend.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/events")

public class EventController {
    @Autowired
    EventRepository eventRepo;

    @PreAuthorize("hasAnyAuthority('STUDENT', 'FACULTY', 'ADMIN')")
    @PostMapping
    public ResponseEntity<Event> createEvent(@RequestBody Event event) {
        return ResponseEntity.ok(eventRepo.save(event));
    }

    @PreAuthorize("hasAnyAuthority('STUDENT', 'FACULTY', 'ADMIN')")
    @GetMapping
    public ResponseEntity<List<Event>> findAllEvents() {
        return ResponseEntity.ok(eventRepo.findAll());
    }

    @PreAuthorize("hasAnyAuthority('STUDENT', 'FACULTY', 'ADMIN')")
    @GetMapping("/by-date")
    public ResponseEntity<List<Event>> findEventsByDate(@RequestParam("date") LocalDate date) {
        return ResponseEntity.ok(eventRepo.findByDate(date));
    }

    @PreAuthorize("hasAnyAuthority('STUDENT', 'FACULTY', 'ADMIN')")
    @GetMapping("/monthly")
    public ResponseEntity<List<Event>> findEventsByMonth(
            @RequestParam("year") int year, 
            @RequestParam("month") int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        return ResponseEntity.ok(eventRepo.findByDateBetween(startDate, endDate));
    }

    @PreAuthorize("hasAnyAuthority('STUDENT', 'FACULTY', 'ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Event> updateEvent(@PathVariable("id") Long id, @RequestBody Event updateEvent) {
        return eventRepo.findById(id)
                .map(event -> {
                    event.setTitle(updateEvent.getTitle());
                    event.setTime(updateEvent.getTime());
                    event.setDate(updateEvent.getDate());
                    return ResponseEntity.ok(eventRepo.save(event));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAnyAuthority('STUDENT', 'FACULTY', 'ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable("id") Long id) {
        eventRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
