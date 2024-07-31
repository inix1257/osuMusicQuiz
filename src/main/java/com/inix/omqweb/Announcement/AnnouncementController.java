package com.inix.omqweb.Announcement;

import com.inix.omqweb.Game.GameManager;
import com.inix.omqweb.osuAPI.Player;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/announcement")
@RequiredArgsConstructor
public class AnnouncementController {
    private final AnnouncementService announcementService;
    private final GameManager gameManager;

    @GetMapping
    public ResponseEntity<?> getAnnouncements() {
        return ResponseEntity.ok(announcementService.getAnnouncements());
    }
}
