package com.inix.omqweb.Daily;

import com.inix.omqweb.Game.ResourceService;
import com.inix.omqweb.Message.Message;
import com.inix.omqweb.Util.AESUtil;
import com.inix.omqweb.osuAPI.Player;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DailyGuessController {
    private final DailyGuessManager dailyGuessManager;
    private final ResourceService resourceService;

    private final AESUtil aesUtil;

    @GetMapping("/daily")
    public DailyGuessDTO getDailyGuessBase64() {
        DailyGuess dailyGuess = dailyGuessManager.getFirstDailyGuessAfterDate();

        if (dailyGuess == null) {
            // No daily guess found
            return null;
        }

        int beatmapId = dailyGuess.getBeatmap().getBeatmapset_id();

        resourceService.getAudio(beatmapId);
        resourceService.getImage(beatmapId, dailyGuess.getBeatmap().isBlur());

        return DailyGuessDTO.builder()
                .base64(aesUtil.encrypt(String.valueOf(beatmapId)))
                .dailyNumber(dailyGuess.getId())
                .build();
    }

    @MessageMapping("/daily")
    public void dailyChat(Message message) {
        dailyGuessManager.submitGuess(message.getSenderId(), message.getContent());
    }
}
