package com.inix.omqweb.Beatmap;

import com.fasterxml.jackson.annotation.JsonBackReference;
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
    @EmbeddedId
    private BeatmapStatsId id;

    @ManyToOne
    @JoinColumn(name = "beatmapset_id", referencedColumnName = "beatmapset_id", insertable = false, updatable = false)
    @JsonBackReference
    private Beatmap beatmap;

    private int guessed;
    private int played;
}