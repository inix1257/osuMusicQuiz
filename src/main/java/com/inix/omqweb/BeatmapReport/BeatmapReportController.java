package com.inix.omqweb.BeatmapReport;

import com.inix.omqweb.Beatmap.BeatmapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BeatmapReportController {
    private final BeatmapService beatmapService;

    @PostMapping("/reportBeatmap")
    public void reportBeatmap(@RequestBody BeatmapReportDTO beatmapReportDTO) {
        beatmapService.reportBeatmap(beatmapReportDTO);
    }

    @GetMapping("/beatmapReports")
    public ResponseEntity<List<BeatmapReportResponseDTO>> getBeatmapReports() {
        return ResponseEntity.ok(beatmapService.getMostReportedBeatmaps());
    }

    @PostMapping("/addBlur")
    public void addBlur(@RequestParam String beatmapsetId) {
        beatmapService.addBlur(beatmapsetId);
    }

    @PostMapping("/deleteReport")
    public void deleteReport(@RequestParam String beatmapsetId) {
        beatmapService.deleteReport(beatmapsetId);
    }
}
