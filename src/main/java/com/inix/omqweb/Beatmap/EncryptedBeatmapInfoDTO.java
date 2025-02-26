package com.inix.omqweb.Beatmap;

import lombok.Data;

@Data
public class EncryptedBeatmapInfoDTO {
    private String base64;
    private String beatmapId;
    private boolean blur;
}
