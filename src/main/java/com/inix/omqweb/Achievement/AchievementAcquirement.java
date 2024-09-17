package com.inix.omqweb.Achievement;

import com.inix.omqweb.osuAPI.Player;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
@Table(name = "achievements_acquirement")
public class AchievementAcquirement {
    @Id
    private int id;

    @ManyToOne
    @JoinColumn(name = "player_id", referencedColumnName = "id")
    private Player player;

    @ManyToOne
    @JoinColumn(name = "achievement_id", referencedColumnName = "id")
    private Achievement achievement;
}
