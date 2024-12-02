package com.inix.omqweb.osuAPI;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.inix.omqweb.Game.GameDifficulty;
import com.inix.omqweb.Game.GameMode;
import com.inix.omqweb.Game.GuessMode;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "player_stats")
public class PlayerStats implements Serializable {
    @EmbeddedId
    private PlayerStatsId id;

    @ManyToOne
    @JoinColumn(name = "player_id", referencedColumnName = "id", insertable = false, updatable = false)
    @JsonBackReference
    private Player player;

    @Enumerated(EnumType.STRING)
    @Column(insertable = false, updatable = false, name = "game_mode")
    private GameMode gameMode;

    @Enumerated(EnumType.STRING)
    @Column(insertable = false, updatable = false, name = "guess_mode")
    private GuessMode guessMode;

    @Enumerated(EnumType.STRING)
    @Column(insertable = false, updatable = false, name = "difficulty")
    private GameDifficulty difficulty;

    private int guessed;
    private int played;
}