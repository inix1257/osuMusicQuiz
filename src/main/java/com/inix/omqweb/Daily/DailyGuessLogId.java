package com.inix.omqweb.Daily;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Embeddable
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyGuessLogId {
    private String player_id;
    private int daily_guess_id;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DailyGuessLogId)) return false;
        DailyGuessLogId that = (DailyGuessLogId) o;
        return player_id.equals(that.player_id) && daily_guess_id == that.daily_guess_id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(player_id, daily_guess_id);
    }
}
