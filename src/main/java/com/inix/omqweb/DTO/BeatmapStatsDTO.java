package com.inix.omqweb.DTO;

import com.inix.omqweb.Game.GameDifficulty;
import com.inix.omqweb.Game.GuessMode;
import lombok.*;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
public class BeatmapStatsDTO {
    private GuessMode guess_mode;
    private GameDifficulty difficulty;
    private int guessed;
    private int played;
    private double guess_rate;
}
