package com.inix.omqweb.Daily;

import com.inix.omqweb.osuAPI.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface DailyGuessLogRepository extends JpaRepository<DailyGuessLog, DailyGuessLogId> {
    @Query(value = "SELECT * FROM daily_guess_log dgl WHERE dgl.player_id = :playerId AND dgl.daily_guess_id = :dailyId", nativeQuery = true)
    DailyGuessLog findByPlayerIdAndDailyGuessLogId(String playerId, int dailyId);

    @Query("SELECT dgl FROM DailyGuessLog dgl WHERE DATE(dgl.date) = DATE(?1)")
    List<DailyGuessLog> findByDate(Date date);

    @Query("SELECT dgl FROM DailyGuessLog dgl WHERE dgl.player.id = ?1")
    List<DailyGuessLog> findByPlayerId(String playerId);
}
