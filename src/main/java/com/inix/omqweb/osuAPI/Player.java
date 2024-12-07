package com.inix.omqweb.osuAPI;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.inix.omqweb.Achievement.Achievement;
import com.inix.omqweb.Achievement.AchievementAcquirement;
import com.inix.omqweb.DTO.PlayerStatsDTO;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "player")
public class Player {
    @Id
    private String id;

    private String username;
    private String country_code;
    private String cover_url;
    private String avatar_url;
    private boolean is_guest;
    private BigDecimal points;

    @Temporal(TemporalType.TIMESTAMP)
    @Transient
    private Date join_date;

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @MapKey(name = "id")
    @JsonManagedReference
    @JsonIgnore
    private Map<PlayerStatsId, PlayerStats> playerStats;

    @Transient
    @JsonGetter("playerStats")
    public List<PlayerStatsDTO> getPlayerStatsDTO() {
        return playerStats.values().stream()
                .sorted(Comparator.comparing((PlayerStats stats) -> stats.getGameMode().getValue())
                        .thenComparing(stats -> stats.getGuessMode().getValue())
                        .thenComparing(stats -> stats.getDifficulty().getValue()))
                .map(stats -> PlayerStatsDTO.builder()
                        .gameMode(stats.getGameMode().toString())
                        .guessMode(stats.getGuessMode().toString())
                        .difficulty(stats.getDifficulty().toString())
                        .guessed(stats.getGuessed())
                        .played(stats.getPlayed())
                        .build())
                .collect(Collectors.toList());
    }

    private boolean ban;

    @Temporal(TemporalType.TIMESTAMP)
    private Date register_date;

    @Temporal(TemporalType.TIMESTAMP)
    private Date last_played;

    @Transient
    private int rank;

    @ManyToOne
    @JoinColumn(name = "current_title_achievement", referencedColumnName = "id")
    private Achievement current_title_achievement;

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    @JsonIgnore
    private List<AchievementAcquirement> achievements;

    @Transient
    @JsonGetter("achievements")
    public List<Achievement> getAchievements() {
        return achievements.stream()
                .map(AchievementAcquirement::getAchievement)
                .collect(Collectors.toList());
    }

    @Transient
    @JsonIgnore
    private Date last_activity;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return id.equals(player.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Transient
    @JsonGetter("level")
    public int level() {
        return (int) Math.sqrt(points.doubleValue());
    }
}
