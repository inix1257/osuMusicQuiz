package com.inix.omqweb.osuAPI;

import com.inix.omqweb.Game.GameDifficulty;
import com.inix.omqweb.Game.GameMode;
import com.inix.omqweb.Game.GuessMode;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PlayerStatsKey implements Serializable {
    private String playerId;
    private GameMode gameMode;
    private GuessMode guessMode;
    private GameDifficulty difficulty;
}