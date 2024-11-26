package com.inix.omqweb.BeatmapReport;

import com.inix.omqweb.Beatmap.Beatmap;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Data
public class BeatmapReportResponseDTO {
    private int beatmapsetId;
    private int reportCount;
    private boolean blur;
    private String artist;
    private String title;
}
