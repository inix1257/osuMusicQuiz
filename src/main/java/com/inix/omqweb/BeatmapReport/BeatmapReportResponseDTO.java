package com.inix.omqweb.BeatmapReport;

import lombok.Data;

@Data
public class BeatmapReportResponseDTO {
    private int beatmapsetId;
    private int reportCount;
    private boolean blur;
    private String artist;
    private String title;
}
