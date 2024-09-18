package com.inix.omqweb.BeatmapReport;

import lombok.Getter;

@Getter
public enum BeatmapReportType {
    MISSING_RESOURCE(1),
    BG_SPOILER(2);

    private final int value;

    BeatmapReportType(int value) {
        this.value = value;
    }

}
