package com.inix.omqweb.Daily;

import com.inix.omqweb.Game.ResourceService;
import com.inix.omqweb.Message.Message;
import com.inix.omqweb.Util.AESUtil;
import com.inix.omqweb.osuAPI.Player;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DailyGuessController {
    private final DailyGuessManager dailyGuessManager;
    private final ResourceService resourceService;

    private final AESUtil aesUtil;

    @GetMapping("/daily")
    public DailyGuessDTO getDailyGuessBase64(HttpServletRequest request, @RequestParam(defaultValue = "-1") int dailyId) {
        Player userInfo = (Player) request.getAttribute("userInfo");
        DailyGuess dailyGuess = (dailyId == -1) ? dailyGuessManager.getTodaysDaily() : dailyGuessManager.getDaily(dailyId);

        if (dailyGuess == null) {
            // No daily guess found
            return null;
        }

        DailyGuessLog dailyGuessLog = null;

        if (userInfo != null) {
            dailyGuessLog = dailyGuessManager.getDailyGuessLog(userInfo, dailyGuess.getId());
        }

        int beatmapId = dailyGuess.getBeatmap().getBeatmapset_id();

        resourceService.getAudio(beatmapId);
        resourceService.getImage(beatmapId, dailyGuess.getBeatmap().isBlur());

        String encryptedBeatmapId = aesUtil.encrypt(String.valueOf(beatmapId));
        if (encryptedBeatmapId == null) {
            return null;
        }
        
        return DailyGuessDTO.builder()
                .base64(encryptedBeatmapId)
                .dailyNumber(dailyGuess.getId())
                .retryCount(dailyGuessLog == null ? 0 : dailyGuessLog.getRetryCount())
                .guessed(dailyGuessLog != null && dailyGuessLog.isGuessed())
                .dailyGuessLog(dailyGuessLog != null && dailyGuessLog.isGuessed() ? dailyGuessLog : null)
                .build();
    }

    @GetMapping("/dailyarchive")
    public List<DailyGuessDTO> getDailyGuessArchive(HttpServletRequest request) {
        Player userInfo = (Player) request.getAttribute("userInfo");
        List<DailyGuessLog> dailyGuessLogs = dailyGuessManager.getDailyGuessLogs(userInfo);

        return dailyGuessLogs.stream()
                .map(dailyGuessLog -> {
                    int beatmapId = dailyGuessLog.getDailyGuess().getBeatmap().getBeatmapset_id();
                    String encryptedBeatmapId = aesUtil.encrypt(String.valueOf(beatmapId));
                    
                    if (encryptedBeatmapId == null) {
                        return null;
                    }

                    return DailyGuessDTO.builder()
                            .base64(encryptedBeatmapId)
                            .dailyNumber(dailyGuessLog.getDailyGuess().getId())
                            .retryCount(dailyGuessLog.getRetryCount())
                            .guessed(dailyGuessLog.isGuessed())
                            .dailyGuessLog(dailyGuessLog.isGuessed() ? dailyGuessLog : null)
                            .build();
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    @MessageMapping("/daily")
    public void dailyChat(Message message) {
        dailyGuessManager.submitGuess(message.getSenderId(), message.getContent(), message.getDailyId());
    }
}
