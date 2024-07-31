package com.inix.omqweb.BeatmapReport;

import lombok.Data;

@Data
public class BeatmapReportDTO {
    private String userId;
    private String encryptedBeatmapsetId;
    private BeatmapReportType beatmapReportType;
}
