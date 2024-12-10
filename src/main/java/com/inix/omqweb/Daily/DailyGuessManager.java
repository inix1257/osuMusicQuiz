package com.inix.omqweb.Daily;

import com.inix.omqweb.Beatmap.Alias.Alias;
import com.inix.omqweb.Beatmap.Alias.AliasRepository;
import com.inix.omqweb.Beatmap.Beatmap;
import com.inix.omqweb.Beatmap.BeatmapRepository;
import com.inix.omqweb.Util.AnswerUtil;
import com.inix.omqweb.osuAPI.Player;
import com.inix.omqweb.osuAPI.PlayerRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class DailyGuessManager {
    private final BeatmapRepository beatmapRepository;
    private final DailyGuessRepository dailyGuessRepository;
    private final DailyGuessLogRepository dailyGuessLogRepository;
    private final PlayerRepository playerRepository;
    private final AliasRepository aliasRepository;

    private final SimpMessagingTemplate template;

    private final ConcurrentHashMap<String, DailyGuessLog> playerGuesses = new ConcurrentHashMap<>();

//    @PostConstruct
    public void init() {
        List<DailyGuess> dailyGuesses = new ArrayList<>();
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(Calendar.YEAR, 2024);
        calendar.set(Calendar.MONTH, Calendar.DECEMBER);
        calendar.set(Calendar.DAY_OF_MONTH, 2);

        for (int i = 0; i < 50; i++) {
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            Date day = calendar.getTime();

            DailyGuess dailyGuess = DailyGuess.builder()
                    .beatmap(beatmapRepository.findRandomBeatmapNotInDailyGuess())
                    .build();

            dailyGuesses.add(dailyGuess);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        dailyGuessRepository.saveAll(dailyGuesses);

        System.out.println("Daily Guesses initialized");
    }

    /*
    This method uses UTC time, reminder to fix if this needs to get changed to local time
     */
    public DailyGuess getFirstDailyGuessAfterDate() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        Date currentDate = calendar.getTime();

        LocalDate currentLocalDate = currentDate.toInstant().atZone(ZoneId.of("UTC")).toLocalDate();

        // Replace this with your specific date
        LocalDate specificDate = LocalDate.of(2024, 11, 18);

        int daysPassed = (int) ChronoUnit.DAYS.between(specificDate, currentLocalDate);

        DailyGuess dailyGuess = dailyGuessRepository.findDailyGuessById(daysPassed + 1);

        List<Alias> aliases = aliasRepository.findAllByBeatmapIds(Collections.singletonList(dailyGuess.getBeatmap().getBeatmapset_id()));

        dailyGuess.getBeatmap().setAliases(aliases);

        return dailyGuess;
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

        if (!correct && beatmap.getAliases() != null) {
            for (Alias alias : beatmap.getAliases()) {
                if (AnswerUtil.checkAnswer(alias.getName(), guess) >= 0.97) {
                    correct = true;
                    break;
                }
            }
        }

        dailyGuessLog.setGuessed(correct);

        if (correct || dailyGuessLog.getRetryCount() >= 5) {
            template.convertAndSend("/daily/" + player.getId() + "/reveal", dailyGuessLog);
        }

        template.convertAndSend("/daily/" + player.getId(), dailyGuessLog.getRetryCount());

        dailyGuessLogRepository.save(dailyGuessLog);
    }

    public DailyGuessLog getDailyGuessLog(Player player) {
        DailyGuess dailyGuess = getFirstDailyGuessAfterDate();
        if (dailyGuess == null) {
            return null;
        }

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Date date = calendar.getTime();

        return dailyGuessLogRepository.findByPlayerIdAndDate(player, date);
    }
}
