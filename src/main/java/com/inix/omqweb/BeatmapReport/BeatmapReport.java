package com.inix.omqweb.BeatmapReport;

import com.inix.omqweb.Beatmap.Beatmap;
import com.inix.omqweb.osuAPI.Player;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Table(name = "report_beatmap")
public class BeatmapReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name="user_id", nullable=false)
    private Player player;

    @ManyToOne
    @JoinColumn(name="beatmapset_id", nullable=false)
    private Beatmap beatmap;

    @Column(name = "report_type")
    private BeatmapReportType beatmapReportType;
}
