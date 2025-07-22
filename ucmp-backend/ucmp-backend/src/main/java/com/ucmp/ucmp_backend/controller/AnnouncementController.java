package com.ucmp.ucmp_backend.controller;

import com.ucmp.ucmp_backend.model.Announcements;
import com.ucmp.ucmp_backend.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import static org.apache.coyote.http11.Constants.a;
import java.util.List;

@RestController
@RequestMapping("/api/announcements")
@CrossOrigin(origins = "http://localhost:5173")  //allow frontend requests
public class AnnouncementController {


    private AnnouncementService service;

        public AnnouncementController(AnnouncementService service) {
          this.service = service;
        }

        @GetMapping("/")
        public List<Announcements> getAll() {
            return service.getAll();
        }

        @PostMapping("/add")
        public Announcements create(@RequestBody Announcements a){
            
            return service.add(a);
        }

        @DeleteMapping("/{id}")
                public void delete(@PathVariable Long id){
            service.delete(id);
        }

        @PutMapping("/{id}")
        public Announcements update(@PathVariable Long id, @RequestBody Announcements a){
            return service.update(id, a);
        }
}
