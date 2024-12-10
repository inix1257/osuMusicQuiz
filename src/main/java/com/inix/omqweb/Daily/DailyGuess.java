package com.inix.omqweb.Daily;

import com.inix.omqweb.Beatmap.Beatmap;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Table(name = "daily_guess")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyGuess {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "beatmapset_id")
    private Beatmap beatmap;
}
