package com.inix.omqweb.Beatmap;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "beatmap_stats")
public class BeatmapStats {
    @Id
    @OneToOne
    @JoinColumn(name = "beatmapset_id", referencedColumnName = "beatmapset_id")
    private Beatmap beatmap;

    private int playcount_title;
    private int playcount_title_answer;
    private int playcount_artist;
    private int playcount_artist_answer;
    private int playcount_creator;
    private int playcount_creator_answer;
}