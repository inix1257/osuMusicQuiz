package com.inix.omqweb.BeatmapReport;

import com.inix.omqweb.Achievement.AchievementService;
import com.inix.omqweb.Beatmap.BeatmapService;
import com.inix.omqweb.osuAPI.Player;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BeatmapReportController {
    private final BeatmapService beatmapService;
    private final AchievementService achievementService;

    private final Logger logger = LoggerFactory.getLogger(BeatmapReportController.class);

    @PostMapping("/reportBeatmap")
    public void reportBeatmap(@RequestBody BeatmapReportDTO beatmapReportDTO) {
        beatmapService.reportBeatmap(beatmapReportDTO);
    }

    @GetMapping("/beatmapReports")
    public ResponseEntity<List<BeatmapReportResponseDTO>> getBeatmapReports(HttpServletRequest request) {
        Player player = (Player) request.getAttribute("userInfo");

        if (achievementService.isModerator(player)) {
            return ResponseEntity.ok(beatmapService.getMostReportedBeatmaps());
        } else {
            logger.warn("Player {} tried to get beatmap reports", player.getId());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/addBlur")
    public ResponseEntity<?> addBlur(@RequestParam String beatmapsetId, HttpServletRequest request) {
        Player player = (Player) request.getAttribute("userInfo");

        if (achievementService.isModerator(player)) {
            beatmapService.addBlur(beatmapsetId);
            return ResponseEntity.ok().build();
        } else {
            logger.warn("Player {} tried to add blur to beatmapset {}", player.getId(), beatmapsetId);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/deleteReport")
    public ResponseEntity<?> deleteReport(@RequestParam String beatmapsetId, HttpServletRequest request) {
        Player player = (Player) request.getAttribute("userInfo");

        if (achievementService.isModerator(player)) {
            beatmapService.deleteReport(beatmapsetId);
            logger.info("Moderator {} deleted report from beatmapset {}", player.getUsername(), beatmapsetId);
            return ResponseEntity.ok().build();
        } else {
            logger.warn("Player {} tried to delete report from beatmapset {}", player.getId(), beatmapsetId);
            return ResponseEntity.badRequest().build();
        }
    }
}
