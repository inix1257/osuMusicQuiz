package com.inix.omqweb.Beatmap;

import lombok.Data;

import java.util.List;

@Data
public class BeatmapPool {
    private List<Beatmap> beatmaps;
    private int totalBeatmapPoolSize;
}
