package com.inix.omqweb.DTO;

import com.inix.omqweb.Game.GuessMode;
import lombok.*;

@Getter
@Setter
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeatmapRangeUpdateDTO {
    private int beatmapset_id;
    private double total_guess_rate;
}