package com.inix.omqweb.osuAPI;

import com.inix.omqweb.Game.GameDifficulty;
import com.inix.omqweb.Game.GameMode;
import com.inix.omqweb.Game.GuessMode;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerStatsId implements Serializable {
    @Column(name = "player_id")
    private String player_id;

    @Column(name = "game_mode")
    private String gameMode;

    @Column(name = "guess_mode")
    private String guessMode;

    @Column(name = "difficulty")
    private String difficulty;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerStatsId that)) return false;
        return player_id.equals(that.player_id) && Objects.equals(gameMode, that.gameMode) && Objects.equals(guessMode, that.guessMode) && Objects.equals(difficulty, that.difficulty);
    }

    @Override
    public int hashCode() {
        return Objects.hash(player_id, gameMode, guessMode, difficulty);
    }
}