package com.inix.omqweb.Achievement;

import com.inix.omqweb.osuAPI.Player;
import com.inix.omqweb.osuAPI.PlayerRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/achievement")
@RequiredArgsConstructor
public class AchievementController {
    private final PlayerRepository playerRepository;
    private final AchievementRepository achievementRepository;
    private final AchievementAcquirementRepository achievementAcquirementRepository;

    @GetMapping
    List<Achievement> getOwnAchievements(HttpServletRequest request) {
        Player player = (Player) request.getAttribute("userInfo");

        List<AchievementAcquirement> achievementAcquirements = achievementAcquirementRepository.findAchievementAcquirementsByPlayer(player);
        return achievementAcquirements.stream()
                .map(AchievementAcquirement::getAchievement)
                .collect(Collectors.toList());
    }

    @PostMapping
    public void updateTitleAchievement(@RequestBody CurrentAchievementUpdateDTO currentAchievementUpdateDTO, HttpServletRequest request) {
        Player player = (Player) request.getAttribute("userInfo");

        if (player == null) {
            throw new RuntimeException("Player not found");
        }

        if (!player.getId().equals(currentAchievementUpdateDTO.getUserId())) {
            throw new RuntimeException("Player ID does not match");
        }

        AchievementAcquirement achievementAcquirement = achievementAcquirementRepository.findAchievementAcquirementsByPlayerAndAchievement(player.getId(), currentAchievementUpdateDTO.getAchievementId());

        if (achievementAcquirement == null) {
            player.setCurrent_title_achievement(null);
        } else {
            player.setCurrent_title_achievement(achievementAcquirement.getAchievement());
        }

        playerRepository.save(player);
    }
}
