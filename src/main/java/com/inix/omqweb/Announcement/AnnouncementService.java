package com.inix.omqweb.Announcement;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnouncementService {
    private final AnnouncementRepository announcementRepository;

    public List<Announcement> getAnnouncements() {
        // Find 3 most recent announcements
        return announcementRepository.findTop3ByOrderByDateDesc();
    }

    public Announcement getAnnouncement(int id) {
        return announcementRepository.findById(id).orElse(null);
    }

    public void deleteAnnouncement(int id) {
        announcementRepository.deleteById(id);
    }
}
