package com.inix.omqweb.Beatmap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.inix.omqweb.Beatmap.Alias.Alias;
import com.inix.omqweb.Game.GameDifficulty;
import com.inix.omqweb.Util.DifficultyCalc;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;
import java.util.List;

@Entity
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "beatmap")
public class Beatmap {
    @Id
    private int beatmapset_id;
    private String artist;
    private String title;
    private String creator;
    private Timestamp approved_date;

    @OneToOne(mappedBy = "beatmap", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = false)
    private BeatmapStats beatmapStats;
//
//    @Column(insertable = false, updatable = false)
//    private double answer_rate;
    private boolean blur;

    private String language;
    private String genre;
    private String tags;

    @Transient
    private GameDifficulty beatmapDifficulty;

    @Transient
    @JsonIgnore
    private int id;

    @Transient
    @JsonIgnore
    private int favourite_count;

    @Transient
    @JsonIgnore
    private float bpm;

    @Transient
    @JsonIgnore
    private int play_count;

    public String getArtistAndTitle() {
        return artist + " - " + title;
    }

    @Override
    public String toString() {
        return getArtistAndTitle() + " alias: " + aliases;
    }

    @Transient
    @JsonIgnore
    List<Alias> aliases;

    @PostLoad
    @PostPersist
    public void setBeatmapDifficulty() {
        beatmapDifficulty = DifficultyCalc.getDifficulty(answer_rate);
    }
}
