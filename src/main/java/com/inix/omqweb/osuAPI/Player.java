package com.inix.omqweb.osuAPI;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.inix.omqweb.Achievement.Achievement;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

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

    private int maps_guessed_easy;
    private int maps_guessed_normal;
    private int maps_guessed_hard;
    private int maps_guessed_insane;

    private int maps_played_easy;
    private int maps_played_normal;
    private int maps_played_hard;
    private int maps_played_insane;

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
