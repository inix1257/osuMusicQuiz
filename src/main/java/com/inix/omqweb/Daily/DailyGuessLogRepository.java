package com.inix.omqweb.Daily;

import com.inix.omqweb.osuAPI.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;

@Repository
public interface DailyGuessLogRepository extends JpaRepository<DailyGuessLog, DailyGuessLogId> {
    @Query("SELECT dgl FROM DailyGuessLog dgl WHERE dgl.player = ?1 AND DATE(dgl.date) = DATE(?2)")
    DailyGuessLog findByPlayerIdAndDate(Player player, Date date);
}
