package com.inix.omqweb.Achievement;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.inix.omqweb.osuAPI.Player;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@RequiredArgsConstructor
@Table(name = "achievements_acquirement")
@AllArgsConstructor
public class AchievementAcquirement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "player_id", referencedColumnName = "id")
    @JsonBackReference
    private Player player;

    @ManyToOne
    @JoinColumn(name = "achievement_id", referencedColumnName = "id")
    private Achievement achievement;
}
