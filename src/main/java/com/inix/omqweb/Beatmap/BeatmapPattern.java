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
@Table(name = "beatmap_pattern")
public class BeatmapPattern {
    @ManyToOne
    @JoinColumn(name = "beatmapset_id", referencedColumnName = "beatmapset_id")
    @JsonBackReference
    private Beatmap beatmap;

    @Id
    private int beatmap_id;
    private String version;
}
