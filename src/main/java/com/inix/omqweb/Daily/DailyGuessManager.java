package com.inix.omqweb.Daily;

import com.inix.omqweb.Beatmap.Beatmap;
import com.inix.omqweb.Beatmap.BeatmapRepository;
import com.inix.omqweb.Util.AnswerUtil;
import com.inix.omqweb.osuAPI.Player;
import com.inix.omqweb.osuAPI.PlayerRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class DailyGuessManager {
    private final BeatmapRepository beatmapRepository;
    private final DailyGuessRepository dailyGuessRepository;
    private final DailyGuessLogRepository dailyGuessLogRepository;
    private final PlayerRepository playerRepository;

    private final SimpMessagingTemplate template;

    private final ConcurrentHashMap<String, DailyGuessLog> playerGuesses = new ConcurrentHashMap<>();

    int dayCount = 0;

//    @PostConstruct
    public void init() {
        List<DailyGuess> dailyGuesses = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.add(Calendar.DAY_OF_YEAR, dayCount++);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            Date day = calendar.getTime();

            DailyGuess dailyGuess = DailyGuess.builder()
                    .date(day)
                    .beatmap(beatmapRepository.findRandomBeatmapNotInDailyGuess())
                    .build();

            dailyGuesses.add(dailyGuess);
        }

        dailyGuessRepository.saveAll(dailyGuesses);
    }

    public DailyGuess getFirstDailyGuessAfterDate() {
        // This method uses UTC time, reminder to fix if this needs to get changed to local time
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        Date currentDate = calendar.getTime();

//        if (date.after(currentDate)) {
//            date = currentDate;
//        }

        return dailyGuessRepository.findFirstByDateAfter(currentDate);
    }

    public void submitGuess(String userId, String guess) {
        DailyGuess dailyGuess = getFirstDailyGuessAfterDate();
        if (dailyGuess == null) {
            return;
        }

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        Date date = calendar.getTime();

        Player player = playerRepository.findById(userId).orElse(null);
        if (player == null) {
            return;
        }

        DailyGuessLog dailyGuessLog = dailyGuessLogRepository.findByPlayerIdAndDate(player, date);

        if (dailyGuessLog == null) {
            dailyGuessLog = DailyGuessLog.builder()
                    .id(new DailyGuessLogId(player.getId(), dailyGuess.getId()))
                    .player(player)
                    .dailyGuess(dailyGuess)
                    .date(date)
                    .guessed(false)
                    .retryCount(0)
                    .build();
        } else if (dailyGuessLog.isGuessed() || dailyGuessLog.getRetryCount() >= 5) {
            template.convertAndSend("/daily/" + player.getId(), dailyGuessLog.getRetryCount());
            template.convertAndSend("/daily/" + player.getId() + "/reveal", dailyGuessLog);
            return;
        }

        dailyGuessLog.setRetryCount(dailyGuessLog.getRetryCount() + 1);

        Beatmap beatmap = dailyGuess.getBeatmap();
        boolean correct = AnswerUtil.checkAnswer(beatmap.getTitle(), guess) >= 0.97;
        dailyGuessLog.setGuessed(correct);

        if (correct || dailyGuessLog.getRetryCount() >= 5) {
            template.convertAndSend("/daily/" + player.getId() + "/reveal", dailyGuessLog);
        }

        template.convertAndSend("/daily/" + player.getId(), dailyGuessLog.getRetryCount());

        dailyGuessLogRepository.save(dailyGuessLog);
    }
}