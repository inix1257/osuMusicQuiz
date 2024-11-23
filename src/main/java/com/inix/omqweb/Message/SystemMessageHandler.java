package com.inix.omqweb.Message;

import com.inix.omqweb.Beatmap.Beatmap;
import com.inix.omqweb.Beatmap.EncryptedBeatmapInfo;
import com.inix.omqweb.DTO.TotalGuessDTO;
import com.inix.omqweb.Game.Game;
import com.inix.omqweb.Game.PlayerAnswer;
import com.inix.omqweb.osuAPI.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class SystemMessageHandler {
    private final SimpMessagingTemplate template;

    private final String ROOM_PREFIX = "/room/";

    private void sendMessage(Game game, String channel, Object content) {
        template.convertAndSend(ROOM_PREFIX + game.getUuid() + "/system/" + channel, content);
    }

    public void onRoomSettingsUpdate(Game game) {
        sendMessage(game, "roomSettings", game);
    }

    public void onPlayersUpdate(Game game) {
        sendMessage(game, "players", game.getPlayers());
    }

    public void onAnnouncement(Game game, String message) {
        sendMessage(game, "announcement", message);
    }

    public void onGameLeaderboardUpdate(Game game, ConcurrentHashMap<String, TotalGuessDTO> leaderboard) {
        sendMessage(game, "gameleaderboard", leaderboard);
    }

    public void onGameStatusUpdate(Game game, String status) {
        sendMessage(game, "gamestatus", status);
    }

    public void onPlayersAnswersUpdate(Game game, ConcurrentHashMap<String, PlayerAnswer> answers) {
        sendMessage(game, "playersAnswers", answers);
    }

    public void onAnswerUpdate(Game game, Beatmap beatmap) {
        sendMessage(game, "answer", beatmap);
    }

    public void onBlurReveal(Game game, String encodedId, String encodedKey) {
        sendMessage(game, "blurReveal", encodedId + "/" + encodedKey);
    }

    public void onBlurReveal(Game game, String key) {
        sendMessage(game, "blurReveal", key);
    }

    public void onGameProgressUpdate(Game game, EncryptedBeatmapInfo encryptedBeatmapInfo) {
        sendMessage(game, "gameprogress", encryptedBeatmapInfo);
    }

    public void onPlayerKick(Game game, Player targetPlayer) {
        sendMessage(game, "playerKick", targetPlayer);
    }

    public void onPlayerInactivityKick(Game game, Player targetPlayer) {
        sendMessage(game, "playerInactivityKick", targetPlayer);
    }

    public void onAnswerSubmissionUpdate(Game game, ConcurrentHashMap<String, Boolean> answerSubmission) {
        sendMessage(game, "answerSubmission", answerSubmission);
    }
}
