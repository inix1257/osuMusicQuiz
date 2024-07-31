package com.inix.omqweb.Beatmap;

import lombok.Data;

import java.util.List;

@Data
public class BeatmapSearchDTO {
    List<Beatmap> beatmapsets;
    String cursor_string;
}
