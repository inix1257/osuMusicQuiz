package com.inix.omqweb.Beatmap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.inix.omqweb.Game.GameDifficulty;
import com.inix.omqweb.Util.DifficultyCalc;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;

@Entity
@Setter
@Getter
@ToString
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
    private int playcount;
    private int playcount_answer;

    @Column(insertable = false, updatable = false)
    private double answer_rate;
    private boolean blur;

    private String language;
    private String genre;
    private String tags;

    @Transient
    private GameDifficulty beatmapDifficulty;

    @Transient
    private int id;

    @Transient
//    @JsonIgnore
    private int favourite_count;

    @Transient
    private float bpm;

    @Transient
//    @JsonIgnore
    private int play_count;

    public String getArtistAndTitle() {
        return artist + " - " + title;
    }

    @Override
    public String toString() {
        return getArtistAndTitle();
    }

    @PostLoad
    @PostPersist
    public void setBeatmapDifficulty() {
        beatmapDifficulty = DifficultyCalc.getDifficulty(playcount, playcount_answer);
    }
}
