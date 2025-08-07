package com.inix.omqweb.BeatmapRequest;

import jakarta.persistence.*;
import lombok.*;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "beatmap_request")
public class BeatmapRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "beatmap_set_id")
    private int beatmapSetId;

    @Column(name = "submitter_id")
    private String submitterId;

    @Column(name = "submitted_at")
    private Timestamp submittedAt;

    @Enumerated(EnumType.STRING)
    private Status status;

    private String artist;
    private String title;
    private String creator;

    @Column(name = "ranked_date")
    private Timestamp rankedDate;

    public enum Status {
        PENDING, APPROVED, REJECTED
    }
}