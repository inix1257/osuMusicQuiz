package com.inix.omqweb.BeatmapReport;

import com.inix.omqweb.Beatmap.BeatmapService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BeatmapReportController {
    private final BeatmapService beatmapService;

    @PostMapping("/reportBeatmap")
    public void reportBeatmap(@RequestBody BeatmapReportDTO beatmapReportDTO) {
        beatmapService.reportBeatmap(beatmapReportDTO);
    }
}
