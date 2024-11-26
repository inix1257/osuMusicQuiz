package com.inix.omqweb.BeatmapReport;

public interface BeatmapReportProjection {
    int getBeatmapsetId();
    int getReportCount();
    boolean isBlur();
    String getArtist();
    String getTitle();
}