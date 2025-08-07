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
@Table(name = "beatmap_request_vote")
public class BeatmapRequestVote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "request_id")
    private BeatmapRequest request;

    @Column(name = "voter_id")
    private String voterId;

    @Column(name = "voted_at")
    private Timestamp votedAt;
}