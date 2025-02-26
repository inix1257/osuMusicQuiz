package com.inix.omqweb.BeatmapReport;

import com.inix.omqweb.Achievement.AchievementService;
import com.inix.omqweb.Beatmap.Beatmap;
import com.inix.omqweb.Beatmap.BeatmapService;
import com.inix.omqweb.Discord.DiscordWebhookService;
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

    private final DiscordWebhookService discordWebhookService;

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
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/addBlur")
    public ResponseEntity<?> addBlur(@RequestParam String beatmapsetId, HttpServletRequest request) {
        Player player = (Player) request.getAttribute("userInfo");

        if (achievementService.isModerator(player)) {
            Beatmap beatmap = beatmapService.addBlur(beatmapsetId);
            discordWebhookService.sendBeatmapWebhook("Beatmap `" + beatmap.getArtistAndTitle() + " (" + beatmap.getBeatmapset_id() + ")` has been blurred by " + player.getUsername());
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/deleteReport")
    public ResponseEntity<?> deleteReport(@RequestParam String beatmapsetId, HttpServletRequest request) {
        Player player = (Player) request.getAttribute("userInfo");

        if (achievementService.isModerator(player)) {
            Beatmap beatmap = beatmapService.deleteReport(beatmapsetId);
            discordWebhookService.sendBeatmapWebhook("Reports for beatmap `" + beatmap.getArtistAndTitle() + " (" + beatmap.getBeatmapset_id() + ")` have been deleted by " + player.getUsername());
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().build();
        }
    }
}
