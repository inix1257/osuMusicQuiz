package com.inix.omqweb.Beatmap;

import com.inix.omqweb.Game.GameMode;
import com.inix.omqweb.Game.GuessMode;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder
public class BeatmapStatsId implements Serializable {
    private int beatmapset_id;

    @Column(name = "game_mode")
    @Enumerated(EnumType.STRING)
    private GameMode game_mode;

    @Column(name = "guess_mode")
    @Enumerated(EnumType.STRING)
    private GuessMode guess_mode;
}
