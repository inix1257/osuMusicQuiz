package com.inix.omqweb.osuAPI;

import com.inix.omqweb.DTO.SeasonalLeaderboardDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

@Repository
public interface PlayerRepository extends JpaRepository<Player, String> {
    List<Player> findByBanFalseOrderByPointsDesc(Pageable pageable);

    int countByPointsGreaterThanAndBanFalse(BigDecimal points);

    @Query(value = "SELECT * FROM Player ORDER BY RAND() LIMIT 10", nativeQuery = true)
    List<Player> findRandomPlayers();

    @Query(value = "SELECT p.id, p.username, p.avatar_url, SUM(hd.difficulty_bonus * hd.speed_bonus * hl.poolsize_bonus * hd.answer) AS recent_points FROM history_detail hd " +
            "JOIN player p ON hd.player_id = p.id " +
            "JOIN history_lobby hl ON hl.session_id = hd.session_id " +
            "WHERE hl.time BETWEEN :startDate AND :endDate " +
            "GROUP BY p.id ORDER BY SUM(hd.difficulty_bonus * hd.speed_bonus * hl.poolsize_bonus * hd.answer) DESC LIMIT 10", nativeQuery = true)
    List<Object[]> getSeasonalLeaderboard(@Param("startDate") Timestamp startDate,
                                                        @Param("endDate") Timestamp endDate);
}
