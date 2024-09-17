package com.inix.omqweb.Achievement;

import com.inix.omqweb.History.LobbyHistoryDetail;
import com.inix.omqweb.osuAPI.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AchievementAcquirementRepository extends JpaRepository<AchievementAcquirement, Integer> {
    List<AchievementAcquirement> findAchievementAcquirementsByPlayer(Player player);

    @Query(value = "SELECT * FROM achievements_acquirement WHERE player_id = :playerId AND achievement_id = :achievementId", nativeQuery = true)
    AchievementAcquirement findAchievementAcquirementsByPlayerAndAchievement(String playerId, int achievementId);
}
