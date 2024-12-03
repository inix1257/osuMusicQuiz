package com.inix.omqweb.Beatmap;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.inix.omqweb.Beatmap.Alias.Alias;
import com.inix.omqweb.DTO.BeatmapStatsDTO;
import com.inix.omqweb.Game.GuessMode;
import com.inix.omqweb.Util.DifficultyCalc;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @OneToMany(mappedBy = "beatmap", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @MapKey(name = "id")
    @JsonManagedReference
    @JsonIgnore
    private Map<BeatmapStatsId, BeatmapStats> beatmapStats;

    private boolean blur;

    private String language;
    private String genre;
    private String tags;

    private boolean deleted;

    @Transient
    private double total_guess_rate;

    @Transient
    @JsonGetter("beatmapStats")
    public List<BeatmapStatsDTO> getBeatmapStatsDTO() {
        return Arrays.stream(GuessMode.values())
                .map(guessMode -> {
                    int totalGuessed = beatmapStats.values().stream()
                            .filter(stats -> stats.getId().getGuess_mode() == guessMode)
                            .mapToInt(BeatmapStats::getGuessed)
                            .sum();
                    int totalPlayed = beatmapStats.values().stream()
                            .filter(stats -> stats.getId().getGuess_mode() == guessMode)
                            .mapToInt(BeatmapStats::getPlayed)
                            .sum();
                    double totalGuessRate = totalPlayed == 0 ? 0 : totalGuessed / (double) totalPlayed;

                    return BeatmapStatsDTO.builder()
                            .guess_mode(guessMode)
                            .difficulty(DifficultyCalc.getDifficulty(guessMode, totalGuessRate))
                            .guessed(totalGuessed)
                            .played(totalPlayed)
                            .guess_rate(totalGuessRate)
                            .build();
                })
                .sorted(Comparator.comparing(BeatmapStatsDTO::getGuess_mode))
                .collect(Collectors.toList());
    }

    @Transient
    private int id;

    @Transient
    private int favourite_count;

    @Transient
    @JsonIgnore
    private float bpm;

    @Transient
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
}
