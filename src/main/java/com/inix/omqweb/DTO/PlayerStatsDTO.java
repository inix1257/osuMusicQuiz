package com.inix.omqweb.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class PlayerStatsDTO {
    private String gameMode;
    private String guessMode;
    private String difficulty;
    private int guessed;
    private int played;
}