package com.inix.omqweb.Daily;

import com.inix.omqweb.osuAPI.Player;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "daily_guess_log")
public class DailyGuessLog {
    @EmbeddedId
    private DailyGuessLogId id;

    @ManyToOne
    @JoinColumn(name = "player_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Player player;

    @ManyToOne
    @JoinColumn(name = "daily_guess_id", insertable = false, updatable = false)
    private DailyGuess dailyGuess;

    private Date date;

    private boolean guessed;

    @Column(name = "retry_count")
    private int retryCount;
}
